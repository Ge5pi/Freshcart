package com.example.foodresq.views

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.adaptersEtc.FeedbackAdapter
import com.example.foodresq.adaptersEtc.ProductAdapter
import com.example.foodresq.classes.Product
import com.example.foodresq.classes.Review
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DetailedActivityRestaurants : Activity() {
    private companion object {
        private const val RC_GOOGLE_SIGN_IN = 4926
        private const val TAG = "DetailedActivityRestaurants"
    }

    private val fireDb = Firebase.firestore
    private lateinit var adapter: FeedbackAdapter
    private val reviews = mutableListOf<Review>()
    private var restId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_restaurants)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup loading animation
        val loading: ImageView = findViewById(R.id.load)
        loading.setBackgroundResource(R.drawable.loading)
        val frameAnimation = loading.background as AnimationDrawable
        loading.post {
            frameAnimation.start()
        }

        // Initialize UI elements
        val restLogo: ImageView = findViewById(R.id.restaurant)
        val restName: TextView = findViewById(R.id.restName)
        val restDesc: TextView = findViewById(R.id.restDesc)
        val feedbackHeader: TextView = findViewById(R.id.feedbackHeader)

        // Set initial visibility
        restDesc.visibility = View.GONE
        restName.visibility = View.GONE
        restLogo.visibility = View.GONE
        feedbackHeader.visibility = View.GONE

        // Get data from intent
        val bundle: Bundle? = intent.extras
        var logoImage = ""
        val restNameBundle = bundle?.getString("restName")
        val desc = bundle?.getString("restDesc")
        val randId = bundle?.getInt("id")

        // Set text values
        restDesc.text = desc
        restName.text = restNameBundle
        val more = findViewById<TextView>(R.id.more)
        more.text = restNameBundle

        // Setup back button
        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }

        // Setup food list
        val foodList: RecyclerView = findViewById(R.id.foodList)
        foodList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val positionList = mutableListOf<Product>()
        val productAdapter = ProductAdapter(positionList, this)
        foodList.adapter = productAdapter

        productAdapter.setOnItemClickListener(object : ProductAdapter.OnItemClickListener {
            override fun onItemClick(id: Int) {
                val intent = Intent(this@DetailedActivityRestaurants, DetailedActivityFood::class.java)
                intent.putExtra("prodId", id)
                startActivity(intent)
            }
        })

        // Setup feedback list
        val reviewList = findViewById<RecyclerView>(R.id.feedbackLayout)
        reviewList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        reviewList.visibility = View.GONE
        adapter = FeedbackAdapter(reviews, this)
        reviewList.adapter = adapter

        // Get restaurant data
        fireDb.collection("restaurants").whereEqualTo("id", randId).get()
            .addOnSuccessListener { restaurants ->
                if (restaurants.isEmpty) {
                    Log.d(TAG, "No restaurant found with id: $randId")
                    hideLoading(loading, frameAnimation, restDesc, restName, restLogo)
                } else {
                    for (doc in restaurants) {
                        restId = doc.id
                        logoImage = doc.getString("ava") ?: ""

                        // Load restaurant image
                        Glide.with(this)
                            .load(logoImage)
                            .into(restLogo)

                        // Now that we have restId, load positions and feedback
                        loadPositions(restId, randId, positionList, productAdapter)
                        loadFeedback(restId, feedbackHeader, reviewList)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting restaurant: ", exception)
                hideLoading(loading, frameAnimation, restDesc, restName, restLogo)
            }
    }

    private fun loadPositions(
        restId: String,
        randId: Int?,
        positionList: MutableList<Product>,
        adapter: ProductAdapter
    ) {
        fireDb.collection("positions").get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "No positions found for restaurant: $randId")
                } else {
                    for (document in documents) {
                        if (document.getString("rest_id") == restId) {
                            val id = document.getLong("id")?.toInt() ?: 0
                            val factId = document.id
                            val name = document.getString("name") ?: ""
                            val description = document.getString("description") ?: ""
                            val image = document.getString("ava") ?: ""
                            val price = document.getLong("price")?.toInt() ?: 0
                            val leftovers = document.getLong("leftovers")?.toInt() ?: 0

                            positionList.add(
                                Product(
                                    id, factId, name, description, image, price, randId!!, leftovers
                                )
                            )
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting positions: ", exception)
            }
    }

    private fun loadFeedback(
        restaurantId: String,
        feedbackHeader: TextView,
        reviewList: RecyclerView
    ) {
        fireDb.collection("feedback")
            .whereEqualTo("rest_id", restaurantId)
            .get()
            .addOnSuccessListener { feedDoc ->
                Log.d(TAG, "Feedback documents count: ${feedDoc.size()}")

                if (feedDoc.isEmpty) {
                    Log.d(TAG, "No feedback found for restaurant: $restaurantId")
                } else {
                    feedbackHeader.visibility = View.VISIBLE
                    reviewList.visibility = View.VISIBLE

                    reviews.clear() // Clear existing reviews

                    for (doc in feedDoc) {
                        val rating = doc.getLong("rating")?.toFloat() ?: 0f
                        val text = doc.getString("body") ?: ""
                        val userId = doc.getString("user_id") ?: ""

                        // Add review directly first for immediate display
                        reviews.add(Review(userId, text, rating, restaurantId))

                        // Optionally fetch user name if needed
                        fireDb.collection("users").document(userId).get()
                            .addOnSuccessListener { userDoc ->
                                val userName = userDoc.getString("name") ?: userId

                                // Find and replace the review with updated user name
                                val index = reviews.indexOfFirst { it.userId == userId && it.text == text }
                                if (index != -1) {
                                    reviews[index] = Review(userName, text, rating, restaurantId)
                                    adapter.notifyItemChanged(index)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error getting user data", e)
                            }
                    }

                    adapter.notifyDataSetChanged()
                }

                // Hide loading after feedback is loaded
                val loading: ImageView = findViewById(R.id.load)
                val frameAnimation = loading.background as AnimationDrawable
                val restDesc: TextView = findViewById(R.id.restDesc)
                val restName: TextView = findViewById(R.id.restName)
                val restLogo: ImageView = findViewById(R.id.restaurant)

                hideLoading(loading, frameAnimation, restDesc, restName, restLogo)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting feedback: ", exception)

                // Hide loading even if there's an error
                val loading: ImageView = findViewById(R.id.load)
                val frameAnimation = loading.background as AnimationDrawable
                val restDesc: TextView = findViewById(R.id.restDesc)
                val restName: TextView = findViewById(R.id.restName)
                val restLogo: ImageView = findViewById(R.id.restaurant)

                hideLoading(loading, frameAnimation, restDesc, restName, restLogo)
            }
    }

    private fun hideLoading(
        loading: ImageView,
        frameAnimation: AnimationDrawable,
        restDesc: TextView,
        restName: TextView,
        restLogo: ImageView
    ) {
        loading.post {
            frameAnimation.stop()
            loading.visibility = View.GONE
            restDesc.visibility = View.VISIBLE
            restName.visibility = View.VISIBLE
            restLogo.visibility = View.VISIBLE
        }
    }
}

