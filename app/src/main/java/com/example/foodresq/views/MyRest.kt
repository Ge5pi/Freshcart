package com.example.foodresq.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.R.id.addPositionButton
import com.example.foodresq.adaptersEtc.ProductAdapter
import com.example.foodresq.classes.Product
import com.example.foodresq.classes.Restaurant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyRest : Activity() {
    private lateinit var auth: FirebaseAuth

    private companion object {
        private const val TAG = "Profile"
        private const val RC_GOOGLE_SIGN_IN = 4926
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_rest)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth
        val fireDb = Firebase.firestore
        var docRest = -1
        var rest: Restaurant = Restaurant("Error Id", 0, "Error name", "Error desc", "error logo")
        val current = auth.currentUser
        fireDb.collection("users").whereEqualTo("email", current?.email).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) Log.d(TAG, "") else {
                    for (document in documents) {
                        docRest = document.getLong("rest_id")?.toInt() ?: 0
                        Log.d(TAG, "docRest: rest_id in user: $docRest")
                    }
                }
            }.addOnSuccessListener {
                fireDb.collection("restaurants").whereEqualTo("id", docRest).get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            docRest = -1
                            Log.d(
                                TAG,
                                "ERROR BLOCK{rest: $rest, docRest: rest_id in user: $docRest}"
                            )
                        } else {
                            for (document in documents) {
                                rest = Restaurant(
                                    document.id,
                                    document.getLong("id")?.toInt() ?: 0,
                                    document.getString("name") ?: "",
                                    document.getString("desc") ?: "",
                                    document.getString("ava") ?: "",
                                )
                                Log.d(TAG, "rest: $rest, docRest: rest_id in user: $docRest")
                            }
                        }
                        Log.d(TAG, "OUTER BLOCK{ rest: $rest, docRest: rest_id in user: $docRest}")
                        updateUI(rest)
                    }
            }
    }

    private fun updateUI(rest: Restaurant) {
        val fireDb = Firebase.firestore
        val restLogo: ImageView = findViewById(R.id.restLogo)

        Glide.with(this)
            .load(rest.logo)
            .into(restLogo)

        val restName: TextView = findViewById(R.id.restName)
        val name = rest.name
        restName.text = name

        val restDesc: TextView = findViewById(R.id.restDesc)
        val desc = rest.desc
        restDesc.text = desc

        val foodList: RecyclerView = findViewById(R.id.foodList)
        foodList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val positionList = mutableListOf<Product>()
        val adapter1 = ProductAdapter(positionList, this)
        foodList.adapter = adapter1

        adapter1.setOnItemClickListener(object : ProductAdapter.OnItemClickListener {
            override fun onItemClick(id: Int) {
                val intent =
                    Intent(this@MyRest, DetailedActivityFood::class.java)
                intent.putExtra("prodId", id)
                intent.putExtra("restId", rest.id)
                startActivity(intent)
                finish()
            }
        })

        fireDb.collection("positions").get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Log.d(TAG, "No documents found. ${rest.id}")
            } else {
                for (document in documents) {
                    Log.d(TAG, "doc: ${document.getString("rest_id")}")
                    if (document.getString("rest_id") == rest.factId) {
                        Log.d(TAG, "rest: ${rest.factId}")
                        val id = document.getLong("id")?.toInt() ?: 0
                        val factId = document.id
                        val nameFact = document.getString("name") ?: ""
                        val description = document.getString("description") ?: ""
                        val image = document.getString("ava") ?: ""
                        val price = document.getLong("price")?.toInt() ?: 0

                        positionList.add(
                            Product(
                                id, factId, nameFact, description, image, price, rest.id
                            )
                        )
                        Log.d(TAG, "position List: $positionList")
                    } else Log.d(TAG, "Ne to: ${document.getString("rest_id")}, vo: ${rest.factId}")
                    adapter1.notifyDataSetChanged()
                }
            }
        }.addOnCompleteListener {
            Log.d(TAG, "PositionList: $positionList")
        }.addOnFailureListener { exception ->
            Log.w(TAG, "Error getting documents: ", exception)
        }


        val addPos: Button = findViewById(addPositionButton)
        addPos.setOnClickListener {
            val intent = Intent(this, AddPosition::class.java)
            intent.putExtra("isRecreate", true)
            startActivity(intent)
            finish()
        }

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }

    }
}
