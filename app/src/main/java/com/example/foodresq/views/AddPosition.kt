package com.example.foodresq.views

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.adaptersEtc.SessionManager
import com.example.foodresq.classes.DbHelper
import com.example.foodresq.classes.Product
import com.example.foodresq.classes.Restaurant
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class AddPosition : ComponentActivity() {
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

        val imagePos = findViewById<ImageView>(R.id.imagePos)
        val initialImageUri = "https://firebasestorage.googleapis.com/v0/b/foodresq-bc5d2.appspot.com/o/empty_avatar.png?alt=media&token=246e42a8-8e6b-4fac-9267-2f85568860e9"
        Glide.with(this)
            .load(initialImageUri)
            .into(imagePos)

        imagePos.setOnClickListener {

        }

        button.setOnClickListener {
            val factName = name.text.trim()
            val factPrice = price.text.trim().toString().toInt()
            val factDesc = desc.text.trim()
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
                }.addOnSuccessListener { openGallery() }.addOnSuccessListener {
                    val intent = Intent(this, Home::class.java)
                    //intent.putExtra("name", factName.toString())
                    //intent.putExtra("desc", factDesc.toString())
                    //intent.putExtra("price", factPrice)
                    startActivity(intent)
                }}
        }
    }



    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadImageToFirestore(uri)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun uploadImageToFirestore(uri: Uri) {
        val avatar = findViewById<ImageView>(R.id.imagePos)
        val storage = Firebase.storage
        val storageRef = uri.lastPathSegment?.let { storage.reference.child(it) }
        val uploadTask = storageRef?.putFile(uri)
        uploadTask?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                Glide.with(this)
                    .load(downloadUri)
                    .into(avatar)
                saveImageToFirestore(downloadUri.toString())
            }
        }
    }

    private fun saveImageToFirestore(downloadUri: String) {
        val bundle = intent.extras
        val name = bundle?.getString("name")
        val desc = bundle?.getString("desc")
        val price = bundle?.getInt("price")
        val fireDb = Firebase.firestore
        val auth = Firebase.auth
        val current = auth.currentUser
        fireDb.collection("positions").whereEqualTo("name", name).whereEqualTo("desc", desc)
            .whereEqualTo("price", price).get().addOnSuccessListener { positions ->
                if (positions.isEmpty) {
                    Log.i("AddPosition", "PROFILE EDIT: CANNOT FIND USER")
                } else {
                    for (pos in positions) {
                        val userRef = fireDb.collection("positions").document(pos.id)
                        userRef.update("ava", downloadUri)
                    }
                }
            }
    }
}
