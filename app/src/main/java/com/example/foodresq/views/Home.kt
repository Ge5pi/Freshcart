package com.example.foodresq.views

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodresq.R
import com.example.foodresq.adaptersEtc.ProductAdapter
import com.example.foodresq.adaptersEtc.RestaurantAdapter
import com.example.foodresq.adaptersEtc.SearchAdapter
import com.example.foodresq.classes.Product
import com.example.foodresq.classes.Restaurant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class Home : Activity() {

    companion object {
        private const val TAG = "Home"
    }
    private lateinit var foodList: RecyclerView
    private lateinit var searchResultsList: RecyclerView
    private lateinit var restList: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var searchResultsContainer: View
    private lateinit var productAdapter: ProductAdapter
    private lateinit var searchResultsAdapter: SearchAdapter
    private lateinit var restaurantAdapter: RestaurantAdapter
    private val positionList = mutableListOf<Product>()
    private val filteredList = mutableListOf<Product>()
    private val restaurantList = mutableListOf<Restaurant>()
    private val fireDb = Firebase.firestore
    private val auth = Firebase.auth
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initViews()
        setupWindowInsets()
        setupRecyclerViews()
        setupClickListeners()
        setupSearchView()
        loadProducts()
        loadRestaurants()
    }

    private fun initViews() {
        searchResultsContainer = findViewById(R.id.searchResultsContainer)
        foodList = findViewById(R.id.upperList)
        searchResultsList = findViewById(R.id.searchResultsList)
        restList = findViewById(R.id.bottomList)
        searchView = findViewById(R.id.search)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerViews() {
        productAdapter = ProductAdapter(positionList, this).apply {
            setOnItemClickListener(object : ProductAdapter.OnItemClickListener {
                override fun onItemClick(id: Int) {
                    navigateToProductDetail(positionList[id])
                }
            })
            setOnAddToCartClickListener(object : ProductAdapter.AddToCartClickListener {
                override fun addToCart(id: Int, toCartButton: Button, inCartButton: Button) {
                    showQuantityDialog(this@Home, id, fireDb, auth)
                }
            })
        }

        searchResultsAdapter = SearchAdapter(filteredList, this).apply {
            setOnItemClickListener(object : SearchAdapter.OnItemClickListener {
                override fun onItemClick(id: Int) {
                    navigateToProductDetail(filteredList[id])
                }
            })
            setOnAddToCartClickListener(object : SearchAdapter.AddToCartClickListener {
                override fun addToCart(id: Int, toCartButton: Button) {
                    showQuantityDialog(this@Home, id, fireDb, auth)
                }
            })
        }

        restaurantAdapter = RestaurantAdapter(restaurantList, this).apply {
            setOnItemClickListener(object : RestaurantAdapter.onItemClickListener {
                override fun onItemClick(position: Int) {
                    navigateToRestaurantDetail(position)
                }
            })
        }

        foodList.apply {
            layoutManager = LinearLayoutManager(this@Home, LinearLayoutManager.HORIZONTAL, false)
            adapter = productAdapter
        }

        searchResultsList.apply {
            layoutManager = LinearLayoutManager(this@Home)
            adapter = searchResultsAdapter
        }

        restList.apply {
            layoutManager = LinearLayoutManager(this@Home, LinearLayoutManager.VERTICAL, false)
            adapter = restaurantAdapter
        }
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.binTopper).setOnClickListener {
            startActivity(Intent(this, CartList::class.java))
        }

        findViewById<ImageView>(R.id.navUser).setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }

        findViewById<ImageView>(R.id.backButtonSearch).setOnClickListener {
            searchView.clearFocus()
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterProducts(it) }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterProducts(it) }
                return false
            }
        })

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                setViewsEnabled(false)
                showSearchResults(searchResultsContainer)
            } else {
                setViewsEnabled(true)
                hideSearchResults(searchResultsContainer)
            }
        }
    }

    private fun loadProducts() {
        ioScope.launch {
            try {
                val restIdMap = getRestaurantIdMap()

                val documents = fireDb.collection("positions")
                    .whereGreaterThan("id", -1)
                    .orderBy("id", Query.Direction.ASCENDING)
                    .get()
                    .await()

                if (documents.isEmpty) {
                    Log.d(TAG, "No products found")
                    return@launch
                }

                val products = documents.mapNotNull { document ->
                    val id = document.getLong("id")?.toInt() ?: return@mapNotNull null
                    val factId = document.id
                    val name = document.getString("name") ?: ""
                    val description = document.getString("description") ?: ""
                    val image = document.getString("ava") ?: ""
                    val price = document.getLong("price")?.toInt() ?: 0
                    val restId = document.getString("rest_id") ?: ""
                    val leftovers = document.getLong("leftovers")?.toInt() ?: 0

                    val restIdNumeric = restIdMap[restId] ?: return@mapNotNull null

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
                }

                withContext(Dispatchers.Main) {
                    positionList.addAll(products)
                    filteredList.addAll(products)
                    productAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading products", e)
            }
        }
    }

    private suspend fun getRestaurantIdMap(): Map<String, Int> {
        return try {
            val restDocs = fireDb.collection("restaurants").get().await()
            restDocs.associate { doc ->
                doc.id.trim() to (doc.getLong("id")?.toInt() ?: 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting restaurant ID map", e)
            emptyMap()
        }
    }

    private fun loadRestaurants() {
        ioScope.launch {
            try {
                val documents = fireDb.collection("restaurants").get().await()

                if (documents.isEmpty) {
                    Log.d(TAG, "No restaurants found")
                    return@launch
                }

                val restaurants = documents.map { document ->
                    val id = document.getLong("id")?.toInt() ?: 0
                    val factId = document.id
                    val name = document.getString("name") ?: ""
                    val desc = document.getString("desc") ?: ""
                    val image = document.getString("ava") ?: ""
                    val position = document.getLong("position")?.toInt() ?: 0

                    Restaurant(factId, id, name, desc, image, position)
                }

                withContext(Dispatchers.Main) {
                    restaurantList.addAll(restaurants)
                    restaurantList.reverse()
                    restaurantAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading restaurants", e)
            }
        }
    }

    private fun navigateToProductDetail(product: Product) {
        val intent = Intent(this, DetailedActivityFood::class.java).apply {
            putExtra("prodId", product.searchId ?: product.id)
            putExtra("product", product.image)
            putExtra("name", product.name)
            putExtra("price", product.price)
            putExtra("desc", product.desc)
            putExtra("restId", product.restId)
        }
        startActivity(intent)
    }

    private fun navigateToRestaurantDetail(position: Int) {
        val intent = Intent(this, DetailedActivityRestaurants::class.java)

        ioScope.launch {
            try {
                val documents = fireDb.collection("restaurants")
                    .whereEqualTo("id", position)
                    .get()
                    .await()

                if (documents.isEmpty) {
                    withContext(Dispatchers.Main) {
                        startActivity(intent)
                    }
                    return@launch
                }

                val document = documents.documents[0]
                intent.putExtra("restaurant", document.getString("ava") ?: "")
                intent.putExtra("restName", document.getString("name") ?: "")
                intent.putExtra("restDesc", document.getString("desc") ?: "")
                intent.putExtra("id", position)

                withContext(Dispatchers.Main) {
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to restaurant detail", e)
                withContext(Dispatchers.Main) {
                    startActivity(intent)
                }
            }
        }
    }

    private fun setViewsEnabled(enabled: Boolean) {
        val bottomList = findViewById<RecyclerView>(R.id.bottomList)
        val upperList = findViewById<RecyclerView>(R.id.upperList)
        val navUser = findViewById<ImageView>(R.id.navUser)

        val visibility = if (enabled) View.VISIBLE else View.GONE
        bottomList.visibility = visibility
        upperList.visibility = visibility
        navUser.visibility = visibility
    }

    private fun filterProducts(query: String) {
        filteredList.clear()

        if (query.isBlank()) {
            filteredList.addAll(positionList)
            searchResultsAdapter.updateList(filteredList)
            return
        }

        var counter = 0
        for (pos in positionList) {
            if (pos.name.contains(query, ignoreCase = true) ||
                pos.desc.contains(query, ignoreCase = true)) {

                val newProd = Product(
                    pos.id - counter,
                    pos.fact_id,
                    pos.name,
                    pos.desc,
                    pos.image,
                    pos.price,
                    pos.restId,
                    pos.leftovers,
                    pos.id
                )
                filteredList.add(newProd)
            } else {
                counter++
            }
        }

        searchResultsAdapter.updateList(filteredList)
    }

    private fun showSearchResults(container: View) {
        val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
        container.visibility = View.VISIBLE
        container.startAnimation(slideDown)
    }

    private fun hideSearchResults(container: View) {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        container.startAnimation(slideUp)
        container.visibility = View.GONE
    }

    private fun showQuantityDialog(
        context: Context,
        productId: Int,
        fireDb: FirebaseFirestore,
        auth: FirebaseAuth
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_quantity_selector, null)
        val editQuantity = dialogView.findViewById<EditText>(R.id.edit_quantity)
        val buttonIncrease = dialogView.findViewById<ImageView>(R.id.button_increase)
        val buttonDecrease = dialogView.findViewById<ImageView>(R.id.button_decrease)
        val buttonAddToCart = dialogView.findViewById<Button>(R.id.button_add_to_cart)

        var productLeft = 0
        var quantity = 1
        editQuantity.setText(quantity.toString())

        ioScope.launch {
            try {
                val docs = fireDb.collection("positions")
                    .whereEqualTo("id", productId)
                    .get()
                    .await()

                if (docs.isEmpty) {
                    Log.d(TAG, "DIALOG: POSITION RECEIVE ERROR")
                    return@launch
                }

                productLeft = docs.documents[0].getLong("leftovers")?.toInt() ?: 0
            } catch (e: Exception) {
                Log.e(TAG, "Error getting product leftovers", e)
            }
        }

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
            val selectedQuantity = editQuantity.text.toString().toIntOrNull() ?: 1

            ioScope.launch {
                try {
                    val positions = fireDb.collection("positions")
                        .whereEqualTo("id", productId)
                        .get()
                        .await()

                    if (positions.isEmpty) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error, please try again later", Toast.LENGTH_SHORT).show()
                        }
                        Log.d(TAG, "HOME(ADD TO CART): POSITION GET ERROR")
                        return@launch
                    }

                    val position = positions.documents[0]
                    val positionId = position.id
                    val leftovers = position.getLong("leftovers")?.toInt() ?: 0

                    if (selectedQuantity <= 0) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Добавить в корзину можно как минимум 1 штуку", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    if (selectedQuantity > leftovers) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Максимально доступное количество: $leftovers", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    fireDb.collection("positions").document(positionId)
                        .update("leftovers", leftovers - selectedQuantity)
                        .await()

                    val users = fireDb.collection("users")
                        .whereEqualTo("email", auth.currentUser?.email)
                        .get()
                        .await()

                    if (users.isEmpty) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error, please try again later", Toast.LENGTH_SHORT).show()
                        }
                        Log.d(TAG, "HOME(ADD TO CART): USER GET ERROR")
                        return@launch
                    }

                    val userId = users.documents[0].id
                    val userDoc = fireDb.collection("users").document(userId).get().await()

                    val currentCart = userDoc.get("cart") as? List<String> ?: listOf()
                    val updatedCart = currentCart.toMutableList()
                    var itemUpdated = false

                    for (i in currentCart.indices) {
                        val cartItem = currentCart[i]
                        val (cartProductId, cartQuantity) = cartItem.split(":")
                        if (cartProductId == positionId) {
                            val newQuantity = cartQuantity.toInt() + selectedQuantity
                            updatedCart[i] = "$cartProductId:$newQuantity"
                            itemUpdated = true
                            break
                        }
                    }

                    if (!itemUpdated) {
                        updatedCart.add("$positionId:$selectedQuantity")
                    }

                    fireDb.collection("users").document(userId)
                        .update("cart", updatedCart)
                        .await()

                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        searchView.clearFocus()
                        Toast.makeText(context, "Product added to cart", Toast.LENGTH_SHORT).show()
                        recreate()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding to cart", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error, please try again later", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }
}