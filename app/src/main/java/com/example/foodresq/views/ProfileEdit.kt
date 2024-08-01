package com.example.foodresq.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.ui.text.input.EditProcessor
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.foodresq.R
import com.example.foodresq.adaptersEtc.SessionManager
import com.example.foodresq.classes.DbHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.FirebaseStorage

class ProfileEdit : ComponentActivity() {

    private companion object{
        private const val TAG = "ProfileEdit"
        private const val RC_GOOGLE_SIGN_IN = 4926
    }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val fireDb = Firebase.firestore
        val storage = Firebase.storage
        auth = Firebase.auth
        val current = auth.currentUser

        val userMail: EditText = findViewById(R.id.userMailEnter)
        val userLogin: EditText = findViewById(R.id.userLoginEnter)
        val avatar: ImageView = findViewById(R.id.profileAvatar)
        fireDb.collection("users").whereEqualTo("email", current?.email).get()
            .addOnSuccessListener { users ->
                if (users.isEmpty) {
                    Log.i(TAG, "PUT IMAGE: USER ERROR")
                } else {
                    for (user in users) {
                        val uriString = user.getString("avatar")
                        val uri = uriString?.toUri()
                        Log.d(TAG, "URI: $uri")
                        if (uri != null) {
                            Glide.with(this)
                                .load(uri)
                                .into(avatar)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user data", e)
            }

        userLogin.setText(current?.displayName.toString())
        userMail.setText(current?.email.toString())
        Log.i(TAG, "User credentials: ${current?.displayName}, ${current?.email}")

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        val buttonSubmit: Button = findViewById(R.id.buttonSubmit)
        buttonSubmit.setOnClickListener {
            val newLogin = userLogin.text.toString().trim()
            val newEmail = userMail.text.toString().trim()
            if (current != null) {
                auth.updateCurrentUser(current).addOnCompleteListener(this){task ->
                    if(task.isSuccessful){
                        Log.d(TAG, "UpdateUserCredentials: success")
                        current.updateEmail(newEmail)
                        val profileUpdate = UserProfileChangeRequest.Builder().setDisplayName(newLogin).build()
                        current.updateProfile(profileUpdate)
                        updateUI(current)
                    }
                }
            }
        }

        avatar.setOnClickListener {
            openGallery()
        }


    }

    private fun updateUI(user: FirebaseUser?) {
        if(user==null){
            Log.w(TAG, "User is null, not going to navigate")
            return
        }

        startActivity(Intent(this, Profile::class.java))
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){uri ->
        if(uri != null){
            uploadImageToFirestore(uri)
        }
    }

    private fun openGallery(){
        galleryLauncher.launch("image/*")
    }


    private fun uploadImageToFirestore(uri: Uri){
        val avatar = findViewById<ImageView>(R.id.profileAvatar)
        val storage = Firebase.storage
        val storageRef = uri.lastPathSegment?.let { storage.reference.child(it) }
        val uploadTask = storageRef?.putFile(uri)

        uploadTask?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener {
                avatar.setImageURI(uri)
                saveImageToFirestore(it.toString())
            }
        }
    }


    private fun saveImageToFirestore(downloadUri: String){
        val fireDb = Firebase.firestore
        var userRef: DocumentReference
        val auth = Firebase.auth
        val current = auth.currentUser
        fireDb.collection("users").whereEqualTo("email", current?.email).get().addOnSuccessListener {users ->
            if(users.isEmpty) Log.i(TAG, "PROFILE EDIT: CANNOT FIND USER")
            else {
                for (user in users) {
                    userRef = fireDb.collection("users").document(user.id)
                    userRef.update("avatar", downloadUri)
                }
            }
        }
    }

}
