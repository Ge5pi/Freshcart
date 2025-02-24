package com.example.foodresq.views

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_restaurants)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
        val fireDb = Firebase.firestore

        val bundle: Bundle? = intent.extras
        var logoImage = ""
        val restNameBundle = bundle?.getString("restName")
        val desc = bundle?.getString("restDesc")
        val randId = bundle?.getInt("id")

        var restId = ""
        fireDb.collection("restaurants").whereEqualTo("id", randId).get().addOnSuccessListener {
            if (it.isEmpty) {
            } else {
                for (doc in it) {
                    restId = doc.id
                    logoImage = doc.getString("ava") ?: ""
                    Glide.with(this)
                        .load(logoImage)
                        .into(restLogo)
                }
            }
        }

        restDesc.text = desc
        restName.text = restNameBundle
        val more = findViewById<TextView>(R.id.more)
        more.text = restNameBundle
//        val logoID = resources.getIdentifier(logoImage, "drawable", packageName)
//        restLogo.setImageResource(logoID)

        val foodList: RecyclerView = findViewById(R.id.foodList)
        foodList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val positionList = mutableListOf<Product>()
        val adapter1 = ProductAdapter(positionList, this)
        foodList.adapter = adapter1

        adapter1.setOnItemClickListener(object : ProductAdapter.OnItemClickListener {
            override fun onItemClick(id: Int) {
                val intent =
                    Intent(this@DetailedActivityRestaurants, DetailedActivityFood::class.java)
                intent.putExtra("prodId", id)
                startActivity(intent)
            }
        })

        fireDb.collection("positions").get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Log.d(TAG, "No documents found. $randId")
            } else {
                for (document in documents) {
                    Log.d(TAG, "doc: ${document.getString("rest_id")}")
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
                    } else Log.d(TAG, "Ne to: ${document.getString("rest_id")}, vo: $restId")
                    adapter1.notifyDataSetChanged()
                }
            }
        }.addOnCompleteListener {
            Log.d(TAG, "PositionList: $positionList")
        }.addOnFailureListener { exception ->
            Log.w(TAG, "Error getting documents: ", exception)
        }

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }

        val reviewList = findViewById<RecyclerView>(R.id.feedbackLayout)
        reviewList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        val reviews = mutableListOf<Review>()
        val adapter = FeedbackAdapter(reviews, this)
        reviewList.adapter = adapter

        fireDb.collection("feedback").get()
            .addOnSuccessListener { feedDoc ->
                Log.d(TAG, "restId: $restId, feedDoc: $feedDoc")
                if (feedDoc.isEmpty) {
                    Log.d(TAG, "FeedDoc is empty")
                } else {
                    for (doc in feedDoc) {
                        if (doc.getString("rest_id") == restId) {
                            Log.d(TAG, "docId: ${doc.id}")
                            val rating = doc.getLong("rating")?.toFloat() ?: 0f
                            val text = doc.getString("body") ?: ""
                            val userId = doc.getString("user_id") ?: ""
                            reviews.add(Review(userId, text, rating, restId))
                        } else {
                            Log.d(TAG, "factual restId: ${doc.getString("rest_id")}")
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }.addOnFailureListener { exception ->
                Log.w(TAG, "Error getting feedback: ", exception)
            }
    }
}

