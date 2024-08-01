package com.example.foodresq.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodresq.R
import com.example.foodresq.adaptersEtc.CartAdapter
import com.example.foodresq.adaptersEtc.CartNamesAdapter
import com.example.foodresq.classes.Product
import com.example.foodresq.classes.Restaurant
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CartList : ComponentActivity() {

    companion object {
        private const val TAG = "CartList"
        var isRecreate = false
        var MAIN_ID = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cart_list)

        val fireDb = Firebase.firestore
        val auth = Firebase.auth
        val currentUserEmail = auth.currentUser?.email


        val cartList = findViewById<RecyclerView>(R.id.cartRecycle)
        val prods = mutableListOf<Product>()
        val adapter = CartAdapter(prods, this)
        cartList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        cartList.adapter = adapter

        val namesList = findViewById<RecyclerView>(R.id.cartNameList)
        val rests = mutableListOf<Restaurant>()
        val adapter1 = CartNamesAdapter(rests, this)
        val restsIds = mutableListOf<Int>()

        namesList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        namesList.adapter = adapter1

        fireDb.collection("users").whereEqualTo("email", currentUserEmail).get()
            .addOnSuccessListener { userDocuments ->
                for (userDoc in userDocuments) {
                    val currentCart = userDoc.get("cart") as? MutableList<String> ?: listOf()
                    for (cartItem in currentCart) {
                        val (cartProdId, cartQuantity) = cartItem.split(":")

                        fireDb.collection("positions").document(cartProdId).get()
                            .addOnSuccessListener { positionDoc ->
                                if (positionDoc.exists()) {
                                    val id = prods.size + 1
                                    val factId = positionDoc.id
                                    val name = positionDoc.getString("name") ?: ""
                                    val desc = positionDoc.getString("desc") ?: ""
                                    val img = positionDoc.getString("ava") ?: ""
                                    val price = positionDoc.getLong("price")?.toInt() ?: 0
                                    val leftovers = cartQuantity.toInt()
                                    val restId = positionDoc.getString("rest_id") ?: ""

                                    fireDb.collection("restaurants").document(restId).get()
                                        .addOnSuccessListener { restDoc ->
                                            if (restDoc.exists()) {
                                                val restaurantId =
                                                    restDoc.getLong("id")?.toInt() ?: 0
                                                prods.add(
                                                    Product(
                                                        id,
                                                        factId,
                                                        name,
                                                        desc,
                                                        img,
                                                        price,
                                                        restaurantId,
                                                        leftovers
                                                    )
                                                )
                                                Log.i(TAG, "prods: $prods")

                                                for (prod in prods) {
                                                    Log.i(TAG, "prod: $prod")
                                                    restsIds.add(prod.restId)
                                                }

                                                for (id in restsIds) {
                                                    fireDb.collection("restaurants")
                                                        .whereEqualTo("id", id).get()
                                                        .addOnSuccessListener { restDocs ->
                                                            if (restDocs.isEmpty) {
                                                                Log.i(TAG, "restDocs are empty")
                                                            } else {
                                                                Log.i(TAG, "restDocs: $restDocs")
                                                                for (doc in restDocs) {
                                                                    Log.i(TAG, "doc id: ${doc.id}")
                                                                    val factId = doc.id
                                                                    val nameInRest =
                                                                        doc.getString("name") ?: ""
                                                                    val desc =
                                                                        doc.getString("desc") ?: ""
                                                                    val logo =
                                                                        doc.getString("ava") ?: ""
                                                                    val position =
                                                                        doc.getLong("position")
                                                                            ?.toInt() ?: 0
                                                                    val rest = Restaurant(
                                                                        factId,
                                                                        id,
                                                                        nameInRest,
                                                                        desc,
                                                                        logo,
                                                                        position
                                                                    )
                                                                    if (rest !in rests) {
                                                                        rests.add(rest)
                                                                    }
                                                                    adapter1.notifyDataSetChanged()
                                                                }
                                                            }
                                                        }
                                                }

                                                adapter.notifyDataSetChanged()
                                            }
                                        }
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting documents: ", exception)
            }

        adapter.setOnDeleteCartClickListener(object : CartAdapter.DeleteCartClickListener {
            override fun onDeleteClick(id: Int, deleteCart: ImageView) {
                val positionId = prods[id].fact_id
                val cartQuantity = prods[id].leftovers

                fireDb.collection("positions").document(positionId).get()
                    .addOnSuccessListener { positionDoc ->
                        val currentLeftovers = positionDoc.getLong("leftovers")?.toInt() ?: 0
                        val newLeftovers = currentLeftovers + cartQuantity
                        fireDb.runTransaction { transaction ->
                            transaction.update(
                                fireDb.collection("positions").document(positionId),
                                "leftovers",
                                newLeftovers
                            )
                            null
                        }.addOnSuccessListener {
                            fireDb.collection("users").whereEqualTo("email", currentUserEmail).get()
                                .addOnSuccessListener { userDocuments ->
                                    for (userDoc in userDocuments) {
                                        val currentCart =
                                            userDoc.get("cart") as? MutableList<String>
                                                ?: mutableListOf()
                                        val updatedCart = currentCart.toMutableList()
                                        updatedCart.removeIf { it.startsWith("$positionId:") }

                                        fireDb.runTransaction { transaction ->
                                            transaction.update(
                                                fireDb.collection("users").document(userDoc.id),
                                                "cart",
                                                updatedCart
                                            )
                                            null
                                        }.addOnSuccessListener {
                                            Log.i(TAG, "Cart updated successfully")
                                            prods.removeAt(id)
                                            adapter.notifyItemRemoved(id)
                                            isRecreate = true
                                        }.addOnFailureListener { e ->
                                            Log.e(TAG, "Error updating cart: ", e)
                                        }
                                    }
                                }.addOnFailureListener { e ->
                                    Log.e(TAG, "Error getting user documents: ", e)
                                }
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "Error updating leftovers: ", e)
                        }
                    }
            }
        })





        adapter1.setOnNameClickListener(object : CartNamesAdapter.OnNameClickListener {
            override fun onNameClick(id: Int) {
                MAIN_ID = id
                recreate()
            }
        })

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            startActivity(Intent(this@CartList, Home::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
