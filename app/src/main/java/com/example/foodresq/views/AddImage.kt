package com.example.foodresq.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class AddImage : ComponentActivity() {

    private companion object {
        private const val TAG = "AddImage"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_image)

        val fireDb = Firebase.firestore

        val imagePos = findViewById<ImageView>(R.id.imagePos)

        // Use Glide to load the initial image
        val initialImageUri = "https://firebasestorage.googleapis.com/v0/b/foodresq-bc5d2.appspot.com/o/empty_avatar.png?alt=media&token=246e42a8-8e6b-4fac-9267-2f85568860e9"
        Glide.with(this)
            .load(initialImageUri)
            .into(imagePos)

        imagePos.setOnClickListener {
            openGallery()
        }

        val addImageButton = findViewById<Button>(R.id.addImageButton)


        addImageButton.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
                // Use Glide to load the new image
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
                    Log.i(TAG, "PROFILE EDIT: CANNOT FIND USER")
                } else {
                    for (pos in positions) {
                        val userRef = fireDb.collection("positions").document(pos.id)
                        userRef.update("ava", downloadUri)
                    }
                }
            }
    }
}
