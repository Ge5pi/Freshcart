package com.example.foodresq.views

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.classes.Product
import com.example.foodresq.classes.Restaurant
import com.example.foodresq.classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class DetailedActivityFood : Activity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var fireDb: FirebaseFirestore
    private lateinit var loading: ImageView
    private lateinit var frameAnimation: AnimationDrawable

    private val views by lazy {
        Views(
            product = findViewById(R.id.product),
            name = findViewById(R.id.name),
            price = findViewById(R.id.price),
            desc = findViewById(R.id.desc),
            restView = findViewById(R.id.rest),
            priceFooter = findViewById(R.id.priceFooter),
            delete = findViewById(R.id.delete),
            backButton = findViewById(R.id.backButton),
            loading = findViewById(R.id.load),
            toCart = findViewById(R.id.toCart),
        )
    }

    private data class Views(
        val product: ImageView,
        val name: TextView,
        val price: TextView,
        val desc: TextView,
        val restView: ImageView,
        val priceFooter: TextView,
        val delete: ImageView,
        val backButton: ImageView,
        val loading: ImageView,
        val toCart: Button,
    )

    companion object {
        private const val TAG = "DetailedActivityFood"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_food)
        setupWindowInsets()
        initializeComponents()
        loadData()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeComponents() {
        auth = Firebase.auth
        fireDb = Firebase.firestore
        setupLoadingAnimation()
        setupBackButton()
    }

    private fun setupLoadingAnimation() {
        with(views.loading) {
            setBackgroundResource(R.drawable.loading)
            frameAnimation = background as AnimationDrawable
            post { frameAnimation.start() }
        }
    }

    private fun setupBackButton() {
        views.backButton.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }
    }

    private fun loadData() {
        val current = auth.currentUser
        val randId = intent.extras?.getInt("restId")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                showLoading()
                val user = async(Dispatchers.IO) { fetchUser(current?.email) }.await()
                val restaurant = async(Dispatchers.IO) { fetchRestaurant(randId) }.await()
                updateUI(restaurant, user)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading data", e)
                Toast.makeText(this@DetailedActivityFood, "Error loading data", Toast.LENGTH_SHORT).show()
            } finally {
                hideLoading()
            }
        }
    }

    private suspend fun fetchUser(email: String?): User = withContext(Dispatchers.IO) {
        val documents = fireDb.collection("users")
            .whereEqualTo("email", email)
            .get()
            .await()

        documents.documents.firstOrNull()?.let { doc ->
            User(
                doc.id,
                doc.getString("login") ?: "",
                doc.getString("email") ?: "",
                doc.getString("password") ?: "",
                rest_id = doc.getLong("rest_id")?.toInt() ?: -1
            )
        } ?: User("Error id", "Error login", "Error email", "Error password", rest_id = -1)
    }

    private suspend fun fetchRestaurant(randId: Int?): Restaurant = withContext(Dispatchers.IO) {
        val documents = fireDb.collection("restaurants")
            .whereEqualTo("id", randId)
            .get()
            .await()

        documents.documents.firstOrNull()?.let { doc ->
            Restaurant(
                doc.id,
                doc.getLong("id")?.toInt() ?: 0,
                doc.getString("name") ?: "",
                doc.getString("desc") ?: "",
                doc.getString("ava") ?: ""
            )
        } ?: Restaurant("Error Id", 0, "Error name", "Error desc", "error logo")
    }

    private fun updateUI(rest: Restaurant, user: User) {
        val prodId = intent.extras?.getInt("prodId") ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                showLoading()
                val product = fetchProduct(prodId)
                displayProductDetails(product, rest, user)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating UI", e)
                Toast.makeText(this@DetailedActivityFood, "Error updating UI", Toast.LENGTH_SHORT).show()
            } finally {
                hideLoading()
            }
        }
    }

    private suspend fun fetchProduct(prodId: Int): Product = withContext(Dispatchers.IO) {
        val documents = fireDb.collection("positions")
            .whereEqualTo("id", prodId)
            .get()
            .await()

        documents.documents.firstOrNull()?.let { doc ->
            Product(
                prodId,
                doc.id,
                doc.getString("name") ?: "",
                doc.getString("desc") ?: "",
                doc.getString("ava") ?: "",
                doc.getLong("price")?.toInt() ?: 0,
                getRest(doc.getString("rest_id") ?: ""),
                doc.getLong("leftovers")?.toInt() ?: 0
            )
        } ?: throw Exception("Product not found")
    }

    @SuppressLint("SetTextI18n")
    private fun displayProductDetails(product: Product, rest: Restaurant, user: User) {
        with(views) {
            Glide.with(this@DetailedActivityFood)
                .load(product.image)
                .into(this.product)

            name.text = product.name
            price.text = product.price.toString() + "₸"
            desc.text = product.desc
            priceFooter.text = product.price.toString() + "₸"

            toCart.setOnClickListener {
                if (product.leftovers > 0) {
                    showQuantityDialog(
                        context = this@DetailedActivityFood,
                        productId = product.id,
                        fireDb = fireDb,
                        auth = auth
                    )
                } else {
                    Toast.makeText(
                        this@DetailedActivityFood,
                        "Товар закончился",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            Glide.with(this@DetailedActivityFood)
                .load(rest.logo)
                .into(restView)

            if (user.rest_id == rest.id) {
                delete.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { showDeleteDialog(product.fact_id) }
                }
            }
        }
    }

    private fun showLoading() {
        views.loading.visibility = View.VISIBLE
        frameAnimation.start()
        hideContentViews()
    }

    private fun hideLoading() {
        views.loading.visibility = View.GONE
        frameAnimation.stop()
        showContentViews()
    }

    private fun hideContentViews() {
        with(views) {
            product.visibility = View.GONE
            name.visibility = View.GONE
            price.visibility = View.GONE
            desc.visibility = View.GONE
            restView.visibility = View.GONE
            priceFooter.visibility = View.GONE
        }
    }

    private fun showContentViews() {
        with(views) {
            product.visibility = View.VISIBLE
            name.visibility = View.VISIBLE
            price.visibility = View.VISIBLE
            desc.visibility = View.VISIBLE
            restView.visibility = View.VISIBLE
            priceFooter.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun showDeleteDialog(prodFactId: String) {
        val fireDb = Firebase.firestore
        val dialogBinding = LayoutInflater.from(this).inflate(R.layout.dialog, null)
        val myDialog = Dialog(this).apply {
            setContentView(dialogBinding)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }

        val noButton: Button = dialogBinding.findViewById(R.id.noButton)
        val yesButton: Button = dialogBinding.findViewById(R.id.yesButton)

        noButton.setOnClickListener {
            myDialog.dismiss()
        }
        yesButton.setOnClickListener {
            val refCollection = Firebase.firestore.collection("positions")
            Log.d(TAG, "Document successfully deleted!")
            Toast.makeText(this, "Товар успешно удален", Toast.LENGTH_SHORT).show()
            val myCurrentDoc = mutableListOf<QueryDocumentSnapshot>()
            fireDb.collection("positions").get().addOnSuccessListener {
                for (doc in it) {
                    if (doc.id == prodFactId)
                        myCurrentDoc.add(doc)
                }
                fireDb.collection("positions").get().addOnSuccessListener { documents ->
                    for (document in documents) {
                        if ((document.getLong("id")?.toInt()
                                ?: 0) > (myCurrentDoc[0].getLong("id")?.toInt() ?: 0)
                        ) {
                            fireDb.runTransaction { transaction ->
                                val newId = (document.getLong("id")?.toInt() ?: 0) - 1
                                Log.d(TAG, "new id: $newId")
                                val posRef = fireDb.collection("positions").document(document.id)
                                transaction.update(posRef, "id", newId)
                                null
                            }
                        } else {
                            Log.d(TAG, "current id: ${document.getLong("id")?.toInt() ?: 0}")
                        }
                    }
                    refCollection.document(prodFactId).delete()
                }
            }.addOnCompleteListener {
                fireDb.collection("counters").get().addOnSuccessListener { counter ->
                    for (count in counter) {
                        fireDb.runTransaction { transaction ->
                            val newCount = (count.getLong("current_id")?.toInt() ?: 0) - 1
                            val countRef = fireDb.collection("counters").document(count.id)
                            transaction.update(countRef, "current_id", newCount)
                            null
                        }
                    }
                    myDialog.dismiss()
                }
            }.addOnSuccessListener {
                intent.putExtra("isRecreate", true)
                startActivity(Intent(this, Home::class.java))
            }


                .addOnFailureListener { e ->
                    Log.w(TAG, "Error deleting document", e)
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getRest(randId: String): Int {
        val fireDb = Firebase.firestore
        var restId = -1
        fireDb.collection("restaurants").document(randId).get().addOnSuccessListener { it ->
            restId = it.getLong("id")?.toInt() ?: 0
        }
        return restId
    }

    private fun showQuantityDialog(
        context: Context,
        productId: Int,
        fireDb: FirebaseFirestore,
        auth: FirebaseAuth
    ) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_quantity_selector, null)
        val editQuantity = dialogView.findViewById<EditText>(R.id.edit_quantity)
        val buttonIncrease = dialogView.findViewById<ImageView>(R.id.button_increase)
        val buttonDecrease = dialogView.findViewById<ImageView>(R.id.button_decrease)
        val buttonAddToCart = dialogView.findViewById<Button>(R.id.button_add_to_cart)

        var productLeft = 0

        fireDb.collection("positions").whereEqualTo("id", productId).get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) Log.d(TAG, "DIALOG: POSITION RECEIVE ERROR")
                else {
                    for (doc in docs) {
                        productLeft = doc.getLong("leftovers")?.toInt() ?: 0
                    }
                }
            }

        var quantity = editQuantity.text.toString().toInt()

        buttonIncrease.setOnClickListener {
            if (quantity < productLeft) {
                quantity++
                editQuantity.setText(quantity.toString())
            }
        }

        buttonDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                editQuantity.setText(quantity.toString())
            }
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        buttonAddToCart.setOnClickListener {
            val selectedQuantity = editQuantity.text.toString().toInt()
            fireDb.collection("positions").whereEqualTo("id", productId).get()
                .addOnSuccessListener { positions ->
                    if (positions.isEmpty) {
                        Toast.makeText(
                            context,
                            "Error, please try again later",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "HOME(ADD TO CART): POSITION GET ERROR")
                    } else {
                        for (position in positions) {
                            if (selectedQuantity <= (position.getLong("leftovers")?.toInt() ?: 0)) {

                                val product = "${position.id}:$selectedQuantity"

                                val leftRef = fireDb.collection("positions").document(position.id)

                                leftRef.update(
                                    "leftovers",
                                    (position.getLong("leftovers")?.toInt() ?: 0) - selectedQuantity
                                )
                                fireDb.collection("users")
                                    .whereEqualTo("email", auth.currentUser?.email)
                                    .get()
                                    .addOnSuccessListener { users ->
                                        if (users.isEmpty) {
                                            Log.d(TAG, "HOME(ADD TO CART): USER GET ERROR")
                                            Toast.makeText(
                                                context,
                                                "Error, please try again later",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            for (user in users) {
                                                val userId = user.id
                                                val cartRef =
                                                    fireDb.collection("users").document(userId)

                                                cartRef.get().addOnSuccessListener { document ->
                                                    val currentCart =
                                                        document.get("cart") as? List<String>
                                                            ?: listOf()
                                                    var updatedCart = currentCart.toMutableList()
                                                    var itemUpdated = false

                                                    for (i in currentCart.indices) {
                                                        val cartItem = currentCart[i]
                                                        val (cartProductId, cartQuantity) = cartItem.split(
                                                            ":"
                                                        )
                                                        if (cartProductId == position.id) {
                                                            val newQuantity =
                                                                cartQuantity.toInt() + selectedQuantity
                                                            updatedCart[i] =
                                                                "$cartProductId:$newQuantity"
                                                            itemUpdated = true
                                                            break
                                                        }
                                                    }

                                                    if (!itemUpdated) {
                                                        updatedCart.add(product)
                                                    }

                                                    cartRef.update("cart", updatedCart)
                                                        .addOnSuccessListener {
                                                            dialog.dismiss()
                                                            Log.d(
                                                                TAG,
                                                                "(ADD TO CART): CART UPDATED SUCCESSFULLY"
                                                            )

                                                            recreate()
                                                            Toast.makeText(
                                                                context,
                                                                "Product added to cart",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Log.w(
                                                                TAG,
                                                                "(ADD TO CART): CART UPDATE FAILED",
                                                                e
                                                            )
                                                            Toast.makeText(
                                                                context,
                                                                "Error, please try again later",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w(TAG, "HOME(ADD TO CART): USER FETCH FAILED", e)
                                        Toast.makeText(
                                            context,
                                            "Error, please try again later",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else if (selectedQuantity > (position.getLong("leftovers")?.toInt()
                                    ?: 0)
                            ) {
                                Toast.makeText(
                                    this@DetailedActivityFood, "Максимально доступное количество: ${
                                        position.getLong("leftovers")
                                            ?.toInt() ?: 0
                                    }", Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@DetailedActivityFood,
                                    "Добавить в корзину можно как минимум 1 штуку",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "(ADD TO CART): POSITION FETCH FAILED", e)
                    Toast.makeText(
                        context,
                        "Error, please try again later",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        dialog.show()
    }
}
