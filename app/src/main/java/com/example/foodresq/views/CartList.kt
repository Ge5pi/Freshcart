package com.example.foodresq.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodresq.R
import com.example.foodresq.adapters.CartAdapter
import com.example.foodresq.adapters.CartNamesAdapter
import com.example.foodresq.viewmodels.CartViewModel

class CartList : ComponentActivity() {
    private val viewModel: CartViewModel by viewModels()
    private lateinit var cartAdapter: CartAdapter
    private lateinit var restaurantAdapter: CartNamesAdapter

    companion object {
        var MAIN_ID = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart_list)

        setupViews()
        setupObservers()
        viewModel.loadCart()
    }

    private fun setupViews() {
        // Setup Cart RecyclerView
        findViewById<RecyclerView>(R.id.cartRecycle).apply {
            cartAdapter = CartAdapter(mutableListOf(), this@CartList)
            layoutManager = LinearLayoutManager(this@CartList)
            adapter = cartAdapter
        }

        // Setup Restaurant Names RecyclerView
        findViewById<RecyclerView>(R.id.cartNameList).apply {
            restaurantAdapter = CartNamesAdapter(mutableListOf(), this@CartList)
            layoutManager = LinearLayoutManager(this@CartList, LinearLayoutManager.HORIZONTAL, false)
            adapter = restaurantAdapter
        }

        // Setup back button
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            startActivity(Intent(this@CartList, Home::class.java))
        }

        // Setup delete listener
        cartAdapter.setOnDeleteCartClickListener { position, product ->
            viewModel.removeFromCart(product)
        }

        // Setup restaurant selection listener
        restaurantAdapter.setOnNameClickListener { restaurantId ->
            MAIN_ID = restaurantId
            viewModel.setSelectedRestaurant(restaurantId)
        }
    }

    private fun setupObservers() {
        viewModel.cartItems.observe(this) { items ->
            cartAdapter.updateItems(items)
        }

        viewModel.restaurants.observe(this) { restaurants ->
            restaurantAdapter.updateItems(restaurants)
        }

        viewModel.selectedRestaurantId.observe(this) { id ->
            MAIN_ID = id
            cartAdapter.notifyDataSetChanged()
        }

        viewModel.totalPrice.observe(this) { total ->
            findViewById<TextView>(R.id.total).text = "₸$total"
        }

        viewModel.loading.observe(this) { isLoading ->
            findViewById<View>(R.id.loadingView).visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }
    }
}