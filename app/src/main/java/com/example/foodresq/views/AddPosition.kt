package com.example.foodresq.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.foodresq.R
import com.example.foodresq.adaptersEtc.SessionManager
import com.example.foodresq.classes.DbHelper
import com.example.foodresq.classes.Product
import com.example.foodresq.classes.Restaurant
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddPosition : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_position)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val name: TextView = findViewById(R.id.name)
        val price: TextView = findViewById(R.id.price)
        val desc: TextView = findViewById(R.id.desc)
        val button: Button = findViewById(R.id.addPositionButton)
        val fireDb = Firebase.firestore
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        val db = DbHelper(this, null)
        val sessionManager = SessionManager(this)
        val userEmail = sessionManager.getUserEmail()
        val current = userEmail?.let { db.getUserByEmail(it) }
        val rest: Restaurant? = current?.let { db.getRest(it.rest_id) }
        val positionCollection = fireDb.collection("positions")
        var currentRestFactId = ""
        var currentRestId = 0

        button.setOnClickListener {
            val factName = name.text.trim()
            val factPrice = price.text.trim().toString().toInt()
            val factDesc = desc.text.trim()
//            if(factName!="" && factPrice!= "" && factDesc != ""){
//                if (rest != null) {
//                    db.addPosition(
//                        factName.toString(),
//                        factDesc.toString(),
//                        "",
//                        Integer.parseInt(factPrice.toString()),
//                    )
//                }
//                else{
//                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_LONG).show()
//                }
//            }

            fireDb.collection("users").whereEqualTo("email", currentUser?.email).get()
                .addOnSuccessListener { documentsUser ->
                    if (documentsUser.isEmpty) {
                        Log.d("AddPosition", "POCHEMU DOCI PUSTYE")
                    } else {
                        for (docUser in documentsUser) {
                            currentRestId = docUser.getLong("rest_id")?.toInt() ?: 0
                            Log.d("AddPosition", "user id: ${docUser.id}, rest_id in user: ${docUser.getLong("rest_id")?.toInt() ?: 0}")

                        }
                    }
                }.addOnCompleteListener {
                    fireDb.collection("counters").document("counter_pos").get().addOnSuccessListener { counter ->
                            val currentPositionId = counter.getLong("current_id")?.toInt() ?: 0
                            fireDb.collection("restaurants").whereEqualTo("id", currentRestId).get()
                                .addOnSuccessListener { restDocs ->
                                    for (doc in restDocs) {
                                        currentRestFactId = doc.id
                                        Log.d("AddPosition", "currentRestId in rest: ${doc.id}")
                                        val dataMap = hashMapOf(
                                            "id" to currentPositionId + 1,
                                            "name" to factName.toString(),
                                            "price" to factPrice,
                                            "desc" to factDesc.toString(),
                                            "leftovers" to 10,
                                            "ava" to "",
                                            "rest_id" to currentRestFactId,
                                        )
                                        positionCollection.add(dataMap)
                                        fireDb.runTransaction { transaction ->
                                            val newIdCounter = currentPositionId + 1
                                            val counterRef = fireDb.collection("counters").document("counter_pos")
                                            transaction.update(counterRef, "current_id", newIdCounter)
                                            null
                                        }
                                    }
                                }
                }.addOnSuccessListener {
                    val intent = Intent(this, AddImage::class.java)
                    intent.putExtra("name", factName.toString())
                    intent.putExtra("desc", factDesc.toString())
                    intent.putExtra("price", factPrice)
                    startActivity(intent)
                }}
        }
    }
}
