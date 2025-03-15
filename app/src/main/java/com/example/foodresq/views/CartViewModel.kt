package com.example.foodresq.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodresq.classes.Product
import com.example.foodresq.classes.Restaurant
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CartViewModel : ViewModel() {
    private val fireDb = Firebase.firestore
    private val auth = Firebase.auth

    private val _cartItems = MutableLiveData<List<Product>>()
    val cartItems: LiveData<List<Product>> = _cartItems

    private val _restaurants = MutableLiveData<List<Restaurant>>()
    val restaurants: LiveData<List<Restaurant>> = _restaurants

    private val _selectedRestaurantId = MutableLiveData<Int>()
    val selectedRestaurantId: LiveData<Int> = _selectedRestaurantId

    private val _totalPrice = MutableLiveData<Double>()
    val totalPrice: LiveData<Double> = _totalPrice

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun loadCart() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val currentUserEmail = auth.currentUser?.email
                val products = mutableListOf<Product>()
                val restaurants = mutableListOf<Restaurant>()

                // Load cart items
                val userDoc = fireDb.collection("users")
                    .whereEqualTo("email", currentUserEmail)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()

                val cart = userDoc?.get("cart") as? List<String> ?: listOf()

                // Load products and restaurants
                cart.forEach { cartItem ->
                    val (prodId, quantity) = cartItem.split(":")
                    val productDoc = fireDb.collection("positions")
                        .document(prodId)
                        .get()
                        .await()

                    if (productDoc.exists()) {
                        val restId = productDoc.getString("rest_id") ?: ""
                        val restDoc = fireDb.collection("restaurants")
                            .document(restId)
                            .get()
                            .await()

                        // Create Product
                        val product = Product(
                            id = products.size + 1,
                            fact_id = productDoc.id,
                            name = productDoc.getString("name") ?: "",
                            desc = productDoc.getString("desc") ?: "",
                            image = productDoc.getString("ava") ?: "",
                            price = productDoc.getLong("price")?.toInt() ?: 0,
                            restId = restDoc.getLong("id")?.toInt() ?: 0,
                            leftovers = quantity.toInt()
                        )
                        products.add(product)

                        // Create Restaurant if not exists
                        val restaurant = Restaurant(
                            restDoc.id,
                            restDoc.getLong("id")?.toInt() ?: 0,
                            restDoc.getString("name") ?: "",
                            restDoc.getString("desc") ?: "",
                            restDoc.getString("ava") ?: "",
                            restDoc.getLong("position")?.toInt() ?: 0
                        )
                        if (restaurant !in restaurants) {
                            restaurants.add(restaurant)
                        }
                    }
                }

                _cartItems.value = products
                _restaurants.value = restaurants
                if (restaurants.isNotEmpty() && _selectedRestaurantId.value == null) {
                    _selectedRestaurantId.value = restaurants.first().id
                }
                updateTotalPrice()
            } catch (e: Exception) {
                // Handle error
            } finally {
                _loading.value = false
            }
        }
    }

    fun setSelectedRestaurant(id: Int) {
        _selectedRestaurantId.value = id
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val selectedId = _selectedRestaurantId.value ?: return
        val items = _cartItems.value ?: return

        val total = items
            .filter { it.restId == selectedId }
            .sumOf { it.price * it.leftovers }

        _totalPrice.value = total.toDouble()
    }

    fun removeFromCart(product: Product) {
        viewModelScope.launch {
            try {
                val currentUserEmail = auth.currentUser?.email

                // Update product leftovers
                val productDoc = fireDb.collection("positions")
                    .document(product.fact_id)
                    .get()
                    .await()

                val currentLeftovers = productDoc.getLong("leftovers")?.toInt() ?: 0
                val newLeftovers = currentLeftovers + product.leftovers

                fireDb.collection("positions")
                    .document(product.fact_id)
                    .update("leftovers", newLeftovers)
                    .await()

                // Update user's cart
                val userDoc = fireDb.collection("users")
                    .whereEqualTo("email", currentUserEmail)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()

                if (userDoc != null) {
                    val currentCart = userDoc.get("cart") as? MutableList<String> ?: mutableListOf()
                    val updatedCart = currentCart.filterNot { it.startsWith("${product.fact_id}:") }

                    fireDb.collection("users")
                        .document(userDoc.id)
                        .update("cart", updatedCart)
                        .await()

                    // Reload cart
                    loadCart()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}