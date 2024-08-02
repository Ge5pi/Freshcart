package com.example.foodresq.views

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodresq.R
import com.example.foodresq.adaptersEtc.ProductAdapter
import com.example.foodresq.adaptersEtc.RestaurantAdapter
import com.example.foodresq.classes.Product
import com.example.foodresq.classes.Restaurant
import com.example.foodresq.views.CartList.Companion.isRecreate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class Home : Activity() {

    companion object {
        private const val RC_GOOGLE_SIGN_IN = 4926
        private const val TAG = "Home"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val foodList = findViewById<RecyclerView>(R.id.upperList)
        val editText: SearchView = findViewById(R.id.search)
        val positionList = mutableListOf<Product>()
        val fireDb = Firebase.firestore
        val auth = Firebase.auth

        val binTopper: ImageView = findViewById(R.id.binTopper)
        binTopper.setOnClickListener {
            val intent = Intent(this, CartList::class.java)
            startActivity(intent)
        }

        val adapter1 = ProductAdapter(positionList, this)
        foodList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        foodList.adapter = adapter1

        fireDb.collection("positions").whereGreaterThan("id", -1)
            .orderBy("id", Query.Direction.ASCENDING).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "No documents found")
                } else {
                    val restIdMap = mutableMapOf<String, Int>()

                    fireDb.collection("restaurants").get()
                        .addOnSuccessListener { restDocs ->
                            for (doc in restDocs) {
                                restIdMap[doc.id.trim()] = doc.getLong("id")?.toInt() ?: 0
                            }

                            for (document in documents) {
//                                if ((document.getLong("leftovers")?.toInt() ?: 0) > 0) {
                                    val id = document.getLong("id")?.toInt() ?: 0
                                    val factId = document.id
                                    val name = document.getString("name") ?: ""
                                    val description = document.getString("description") ?: ""
                                    val image = document.getString("ava") ?: ""
                                    val price = document.getLong("price")?.toInt() ?: 0
                                    val restId = document.getString("rest_id") ?: ""
                                    val leftovers = document.getLong("leftovers")?.toInt() ?: 0

                                    val restIdNumeric = restIdMap[restId] ?: continue
                                    positionList.add(
                                        Product(
                                            id,
                                            factId,
                                            name,
                                            description,
                                            image,
                                            price,
                                            restIdNumeric,
                                            leftovers
                                        )
                                    )
//                                }
                                adapter1.notifyDataSetChanged()
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }

        adapter1.setOnItemClickListener(object : ProductAdapter.OnItemClickListener {
            override fun onItemClick(id: Int) {
                val intent = Intent(this@Home, DetailedActivityFood::class.java).apply {
                    putExtra("prodId", id)
                    putExtra("product", positionList[id].image)
                    putExtra("name", positionList[id].name)
                    putExtra("price", positionList[id].price)
                    putExtra("desc", positionList[id].desc)
                    putExtra("restId", positionList[id].restId)
                }
                startActivity(intent)

            }
        })

        adapter1.setOnAddToCartClickListener(object : ProductAdapter.AddToCartClickListener {
            override fun addToCart(id: Int, toCartButton: Button, inCartButton: Button) {
                showQuantityDialog(this@Home, id, fireDb, auth)
            }
        })

        val restList = findViewById<RecyclerView>(R.id.bottomList)
        val rests = mutableListOf<Restaurant>()
        val adapter = RestaurantAdapter(rests, this)
        restList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        restList.adapter = adapter

        fireDb.collection("restaurants").get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "No documents found: Rests")
                } else {
                    for (document in documents) {
                        val id = document.getLong("id")?.toInt() ?: 0
                        val factId = document.id
                        val name = document.getString("name") ?: ""
                        val desc = document.getString("desc") ?: ""
                        val image = document.getString("ava") ?: ""
                        val position = document.getLong("position")?.toInt() ?: 0

                        rests.add(Restaurant(factId, id, name, desc, image, position))
                    }
                    rests.reverse()
                    adapter.notifyDataSetChanged()
                }
            }

        adapter.setOnItemClickListener(object : RestaurantAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val intent = Intent(this@Home, DetailedActivityRestaurants::class.java)
                fireDb.collection("restaurants").whereEqualTo("id", position).get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            startActivity(intent)
                        } else {
                            for (document in documents) {
                                intent.putExtra("restaurant", document.getString("ava") ?: "")
                                intent.putExtra("restName", document.getString("name") ?: "")
                                intent.putExtra("restDesc", document.getString("desc") ?: "")
                                intent.putExtra("id", position)
                            }
                            startActivity(intent)
                        }
                    }
            }
        })

        findViewById<ImageView>(R.id.navUser).setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val search = findViewById<SearchView>(R.id.search)
        

    }

    override fun onStart() {
        super.onStart()
        while (isRecreate) {
            recreate()
            isRecreate = false
        }
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
                                                                "HOME(ADD TO CART): CART UPDATED SUCCESSFULLY"
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
                                                                "HOME(ADD TO CART): CART UPDATE FAILED",
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
                            }
                            else if(selectedQuantity > (position.getLong("leftovers")?.toInt() ?: 0)){
                                Toast.makeText(this@Home, "Максимально доступное количество: ${position.getLong("leftovers")
                                    ?.toInt() ?: 0}", Toast.LENGTH_SHORT).show()
                            }
                            else{
                                Toast.makeText(this@Home, "Добавить в корзину можно как минимум 1 штуку", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "HOME(ADD TO CART): POSITION FETCH FAILED", e)
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

