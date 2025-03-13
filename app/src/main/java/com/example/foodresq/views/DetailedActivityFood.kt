package com.example.foodresq.views

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
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
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DetailedActivityFood : Activity() {
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "DetailedActivityFood"
        private const val RC_GOOGLE_SIGN_IN = 4926
    }

    override fun onCreate(savedInstanceState: Bundle?) {

//        //val loading: ImageView = findViewById(R.id.load)
//        //loading.setBackgroundResource(R.drawable.loading)
//        //val frameAnimation = loading.background as AnimationDrawable
//        //loading.post {
//            frameAnimation.start()
//        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_food)
        auth = Firebase.auth
        val fireDb = Firebase.firestore
        val current = auth.currentUser

        var rest = Restaurant("Error Id", 0, "Error name", "Error desc", "error logo")
        val bundle = intent.extras
        val randId = bundle?.getInt("restId")
        Log.d(TAG, "randId in Detailed: $randId")



        var user = User("Error id", "Error login", "Error email", "Error password", rest_id = -1)
        fireDb.collection("users").whereEqualTo("email", current?.email).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        user = User(
                            document.id,
                            document.getString("login") ?: "",
                            document.getString("email") ?: "",
                            document.getString("password") ?: "",
                            rest_id = document.getLong("rest_id")?.toInt() ?: 0
                        )
                    }
                }
            }.addOnCompleteListener {
                fireDb.collection("restaurants").whereEqualTo("id", randId).get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            for (document in documents) {
                                rest = Restaurant(
                                    document.id,
                                    document.getLong("id")?.toInt() ?: 0,
                                    document.getString("name") ?: "",
                                    document.getString("desc") ?: "",
                                    document.getString("ava") ?: ""
                                )
                                Log.d(TAG, "rest: $rest, RandId: $randId")
                            }
                        }
                        Log.d(TAG, "OUTER BLOCK{ rest: $rest, docRest: rest_id in user: $randId}")
                        updateUI(rest, user)
                    }
            }.addOnCompleteListener {
//                loading.post {
//                    frameAnimation.stop()
//                }
            }
    }

    private fun updateUI(rest: Restaurant, user: User) {
        val product: ImageView = findViewById(R.id.product)
        val name: TextView = findViewById(R.id.name)
        val price: TextView = findViewById(R.id.price)
        val desc: TextView = findViewById(R.id.desc)
        val restView: ImageView = findViewById(R.id.rest)
        val priceFooter: TextView = findViewById(R.id.priceFooter)

        val loading: ImageView = findViewById(R.id.load)
        loading.setBackgroundResource(R.drawable.loading)
        val frameAnimation = loading.background as AnimationDrawable
        loading.post {
            frameAnimation.start()
        }

        product.visibility = View.GONE
        name.visibility = View.GONE
        price.visibility = View.GONE
        desc.visibility = View.GONE
        restView.visibility = View.GONE
        priceFooter.visibility = View.GONE

        val bundle = intent.extras
        val prodId = bundle?.getInt("prodId") ?: return

        val fireDb = Firebase.firestore
        fireDb.collection("positions").whereEqualTo("id", prodId).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val productDetails = documents.map { document ->
                        Product(
                            prodId,
                            document.id,
                            document.getString("name") ?: "",
                            document.getString("desc") ?: "",
                            document.getString("ava") ?: "",
                            document.getLong("price")?.toInt() ?: 0,
                            getRest(document.getString("rest_id") ?: ""),
                            document.getLong("leftovers")?.toInt() ?: 0
                        )
                    }.firstOrNull() ?: return@addOnSuccessListener

                    Glide.with(this)
                        .load(productDetails.image)
                        .into(product)
                    name.text = productDetails.name
                    price.text = productDetails.price.toString()
                    desc.text = productDetails.desc
                    priceFooter.text = productDetails.price.toString()
                    Log.d(TAG, "rest_id in user: ${user.rest_id}, rest id(Int) in rest: ${rest.id}")

                    Glide.with(this)
                        .load(rest.logo)
                        .into(restView)

                    if (user.rest_id == rest.id) {
                        Log.d(
                            TAG,
                            "rest_id in user: ${user.rest_id}, rest id(Int) in rest: ${rest.id}"
                        )
                        findViewById<ImageView>(R.id.delete).apply {
                            visibility = View.VISIBLE
                            setOnClickListener { showDeleteDialog(productDetails.fact_id) }
                        }
                    }
                } else {
                    Log.d(TAG, "No docs found")
                }
            }.addOnCompleteListener {
                loading.post{
                    frameAnimation.stop()
                    loading.visibility = View.GONE
                    product.visibility = View.VISIBLE
                    name.visibility = View.VISIBLE
                    price.visibility = View.VISIBLE
                    desc.visibility = View.VISIBLE
                    restView.visibility = View.VISIBLE
                    priceFooter.visibility = View.VISIBLE
                }
            }

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
}
