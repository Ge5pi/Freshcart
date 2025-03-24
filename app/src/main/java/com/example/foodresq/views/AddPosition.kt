package com.example.foodresq.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.adaptersEtc.PriceRecommender
import com.example.foodresq.adaptersEtc.SessionManager
import com.example.foodresq.classes.DbHelper
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.core.Tag
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class AddPosition : ComponentActivity() {
    private var selectedImageUri: Uri? = null
    private lateinit var imagePos: ImageView
    private lateinit var nameInput: TextView
    private lateinit var priceInput: TextView
    private lateinit var descInput: TextView
    private lateinit var addButton: Button
    private lateinit var quantityInput: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_position)

        setupViews()
        setupWindowInsets()
        initializeImageView()

        val suggest: Button = findViewById(R.id.suggestPriceButton)
        suggest.setOnClickListener {
            val recommender = PriceRecommender(this, this)
            if (nameInput.text.toString() != "") {
                recommender.recommendPrice(
                    newProductName = nameInput.text.toString(),
                ) { recommendedPrice ->
                    if (recommendedPrice != null) {
                        priceInput.setText(recommendedPrice.toString())
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Recommended price: $recommendedPrice₸",
                                Toast.LENGTH_SHORT
                            ).show()
                            priceInput.setText(recommendedPrice.toString())
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                "Could not get price recommendation. Enter name",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Could not get price. enter name", Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun setupViews() {
        imagePos = findViewById(R.id.imagePos)
        nameInput = findViewById(R.id.name)
        priceInput = findViewById(R.id.price)
        descInput = findViewById(R.id.desc)
        addButton = findViewById(R.id.addPositionButton)
        quantityInput = findViewById(R.id.quantity)

        addButton.setOnClickListener {
            if (validateInputs()) {
                addPosition()
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeImageView() {
        val initialImageUri =
            "https://firebasestorage.googleapis.com/v0/b/foodresq-bc5d2.appspot.com/o/empty_avatar.png?alt=media&token=246e42a8-8e6b-4fac-9267-2f85568860e9"
        Glide.with(this)
            .load(initialImageUri)
            .into(imagePos)

        imagePos.setOnClickListener {
            openGallery()
        }
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it
                Glide.with(this)
                    .load(uri)
                    .into(imagePos)
            }
        }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun validateInputs(): Boolean {
        val name = nameInput.text.toString().trim()
        val price = priceInput.text.toString().trim()
        val desc = descInput.text.toString().trim()

        if (name.isEmpty() || price.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            price.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Enter correct price", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "Please, choose image", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun addPosition() {
        val fireDb = Firebase.firestore
        val auth = Firebase.auth
        val storage = Firebase.storage
        val currentUser = auth.currentUser
        val sessionManager = SessionManager(this)
        val db = DbHelper(this, null)

        addButton.isEnabled = false
        addButton.text = "Proceeding..."

        val timestamp = System.currentTimeMillis()
        val imageFileName = "position_${timestamp}.jpg"
        val storageRef = storage.reference.child("positions").child(imageFileName)

        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        getCurrentRestaurantInfo { currentRestId, currentRestFactId ->
                            getNextPositionId { currentPositionId ->
                                savePositionToFirestore(
                                    currentPositionId = currentPositionId,
                                    currentRestFactId = currentRestFactId,
                                    imageUrl = downloadUri.toString()
                                )
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("AddPosition", "Error uploading image", exception)
                    Toast.makeText(this, "Error while uploading image", Toast.LENGTH_SHORT).show()
                    addButton.isEnabled = true
                    addButton.text = "Add"
                }
        }
    }

    private fun getCurrentRestaurantInfo(callback: (Int, String) -> Unit) {
        val fireDb = Firebase.firestore
        val auth = Firebase.auth
        val currentUser = auth.currentUser

        fireDb.collection("users")
            .whereEqualTo("email", currentUser?.email)
            .get()
            .addOnSuccessListener { documentsUser ->
                if (!documentsUser.isEmpty) {
                    val currentRestId = documentsUser.documents[0].getLong("rest_id")?.toInt() ?: 0

                    fireDb.collection("restaurants")
                        .whereEqualTo("id", currentRestId)
                        .get()
                        .addOnSuccessListener { restDocs ->
                            if (!restDocs.isEmpty) {
                                val currentRestFactId = restDocs.documents[0].id
                                callback(currentRestId, currentRestFactId)
                            } else {
                                handleError("Restaurant not found")
                            }
                        }
                        .addOnFailureListener { handleError(it.message) }
                } else {
                    handleError("User not found")
                }
            }
            .addOnFailureListener { handleError(it.message) }
    }

    private fun getNextPositionId(callback: (Int) -> Unit) {
        val fireDb = Firebase.firestore

        fireDb.collection("counters")
            .document("counter_pos")
            .get()
            .addOnSuccessListener { counter ->
                val currentPositionId = counter.getLong("current_id")?.toInt() ?: 0
                callback(currentPositionId + 1)
            }
            .addOnFailureListener { handleError(it.message) }
    }

    private fun savePositionToFirestore(
        currentPositionId: Int,
        currentRestFactId: String,
        imageUrl: String
    ) {
        val fireDb = Firebase.firestore
        val positionCollection = fireDb.collection("positions")

        val dataMap = hashMapOf(
            "id" to currentPositionId,
            "name" to nameInput.text.toString().trim(),
            "price" to priceInput.text.toString().trim().toInt(),
            "desc" to descInput.text.toString().trim(),
            "leftovers" to quantityInput.text.toString().trim().toInt(),
            "ava" to imageUrl,
            "rest_id" to currentRestFactId
        )

        positionCollection.add(dataMap)
            .addOnSuccessListener {
                updatePositionCounter(currentPositionId)
                navigateToHome()
            }
            .addOnFailureListener { handleError(it.message) }
    }

    private fun updatePositionCounter(newId: Int) {
        val fireDb = Firebase.firestore

        fireDb.runTransaction { transaction ->
            val counterRef = fireDb.collection("counters").document("counter_pos")
            transaction.update(counterRef, "current_id", newId)
            null
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(this, Home::class.java))
        finish()
    }

    private fun handleError(message: String?) {
        Log.e("AddPosition", "Error: $message")
        Toast.makeText(this, "Error occurred. Try again.", Toast.LENGTH_SHORT).show()
        addButton.isEnabled = true
        addButton.text = "Add"
    }
}
