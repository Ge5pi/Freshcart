package com.example.foodresq.views

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
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
    private lateinit var reviewList: RecyclerView
    private var restId = ""
    private lateinit var feedbackHeader: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_restaurants)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<Button>(R.id.addReview).setOnClickListener {
            showReviewDialog()
        }


        val loading: ImageView = findViewById(R.id.load)
        loading.setBackgroundResource(R.drawable.loading)
        val frameAnimation = loading.background as AnimationDrawable
        loading.post {
            frameAnimation.start()
        }

        val restLogo: ImageView = findViewById(R.id.restaurant)
        val restName: TextView = findViewById(R.id.restName)
        val restDesc: TextView = findViewById(R.id.restDesc)
        val menuText: TextView = findViewById(R.id.textView4)
        feedbackHeader = findViewById(R.id.feedbackHeader)
        val addReview: Button = findViewById(R.id.addReview)

        restDesc.visibility = View.GONE
        addReview.visibility = View.GONE
        restName.visibility = View.GONE
        menuText.visibility = View.GONE
        restLogo.visibility = View.GONE
        feedbackHeader.visibility = View.GONE

        val bundle: Bundle? = intent.extras
        var logoImage = ""
        val restNameBundle = bundle?.getString("restName")
        val desc = bundle?.getString("restDesc")
        val randId = bundle?.getInt("id")

        restDesc.text = desc
        restName.text = restNameBundle
        val more = findViewById<TextView>(R.id.more)
        more.text = restNameBundle

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }

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

        reviewList = findViewById(R.id.feedbackLayout)
        reviewList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        reviewList.visibility = View.GONE
        adapter = FeedbackAdapter(reviews, this)
        reviewList.adapter = adapter

        fireDb.collection("restaurants").whereEqualTo("id", randId).get()
            .addOnSuccessListener { restaurants ->
                if (restaurants.isEmpty) {
                    Log.d(TAG, "No restaurant found with id: $randId")
                    hideLoading(loading, frameAnimation, restDesc, restName, restLogo, menuText, addReview)
                } else {
                    for (doc in restaurants) {
                        restId = doc.id
                        logoImage = doc.getString("ava") ?: ""

                        Glide.with(this)
                            .load(logoImage)
                            .into(restLogo)

                        loadPositions(restId, randId, positionList, productAdapter)
                        loadFeedback(restId, feedbackHeader, reviewList)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting restaurant: ", exception)
                hideLoading(loading, frameAnimation, restDesc, restName, restLogo, menuText, addReview)
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

                    reviews.clear()

                    for (doc in feedDoc) {
                        val rating = doc.getLong("rating")?.toFloat() ?: 0f
                        val text = doc.getString("body") ?: ""
                        val userId = doc.getString("user_id") ?: ""

                        reviews.add(Review(userId, text, rating, restaurantId))

                        fireDb.collection("users").document(userId).get()
                            .addOnSuccessListener { userDoc ->
                                val userName = userDoc.getString("name") ?: userId

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

                val loading: ImageView = findViewById(R.id.load)
                val frameAnimation = loading.background as AnimationDrawable
                val restDesc: TextView = findViewById(R.id.restDesc)
                val restName: TextView = findViewById(R.id.restName)
                val restLogo: ImageView = findViewById(R.id.restaurant)
                val menuText: TextView = findViewById(R.id.textView4)
                val addReview: Button = findViewById(R.id.addReview)

                hideLoading(loading, frameAnimation, restDesc, restName, restLogo, menuText, addReview)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting feedback: ", exception)

                val loading: ImageView = findViewById(R.id.load)
                val frameAnimation = loading.background as AnimationDrawable
                val restDesc: TextView = findViewById(R.id.restDesc)
                val restName: TextView = findViewById(R.id.restName)
                val restLogo: ImageView = findViewById(R.id.restaurant)
                val menuText: TextView = findViewById(R.id.textView4)
                val addReview: Button = findViewById(R.id.addReview)

                hideLoading(loading, frameAnimation, restDesc, restName, restLogo, menuText, addReview)
            }
    }

    private fun hideLoading(
        loading: ImageView,
        frameAnimation: AnimationDrawable,
        restDesc: TextView,
        restName: TextView,
        restLogo: ImageView,
        menu: TextView,
        addReview: Button,
    ) {
        loading.post {
            frameAnimation.stop()
            loading.visibility = View.GONE
            addReview.visibility = View.VISIBLE
            menu.visibility = View.VISIBLE
            restDesc.visibility = View.VISIBLE
            restName.visibility = View.VISIBLE
            restLogo.visibility = View.VISIBLE
        }
    }
    private fun showReviewDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val reviewText = dialogView.findViewById<EditText>(R.id.reviewText)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val submitButton = dialogView.findViewById<Button>(R.id.submitButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            val review = reviewText.text.toString().trim()

            if (rating == 0f) {
                Toast.makeText(this, "Please, leave a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (review.isEmpty()) {
                Toast.makeText(this, "Please, write a review", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitButton.isEnabled = false
            submitButton.text = "Sending..."

            val auth = Firebase.auth
            val currentUser = auth.currentUser

            if (currentUser == null) {
                Toast.makeText(this, "Need to authorize", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }

            fireDb.collection("users")
                .whereEqualTo("email", currentUser.email)
                .get()
                .addOnSuccessListener { docs ->
                    if (docs.isEmpty) {
                        Log.d("Detailed", "docs are null")
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                        submitButton.isEnabled = true
                        submitButton.text = "Send"
                    } else {
                        var userid = ""
                        for (doc in docs) {
                            userid = doc.id
                            break
                        }

                        val feedbackData = hashMapOf(
                            "user_id" to userid,
                            "rating" to rating.toLong(),
                            "body" to review,
                            "rest_id" to restId
                        )

                        fireDb.collection("feedback")
                            .add(feedbackData)
                            .addOnSuccessListener {
                                dialog.dismiss()
                                Toast.makeText(this, "Thank you for your review!", Toast.LENGTH_SHORT).show()

                                loadFeedback(restId, feedbackHeader, reviewList)
                            }
                            .addOnFailureListener { e ->
                                Log.e("Detailed", "Error adding review", e)
                                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()

                                submitButton.isEnabled = true
                                submitButton.text = "Send"
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Detailed", "Error getting user", e)
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()

                    submitButton.isEnabled = true
                    submitButton.text = "Send"
                }
        }

        dialog.show()
    }

}