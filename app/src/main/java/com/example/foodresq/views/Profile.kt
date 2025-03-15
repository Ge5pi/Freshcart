package com.example.foodresq.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.adaptersEtc.SessionManager
import com.example.foodresq.classes.DbHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class Profile : Activity() {

    private companion object {
        private const val TAG = "Profile"
        private const val RC_GOOGLE_SIGN_IN = 4926
    }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val userMail: TextView = findViewById(R.id.userMail)
        val userLogin: TextView = findViewById(R.id.userLogin)
        val sessionManager = SessionManager(this)
        val userEmail = sessionManager.getUserEmail()
        auth = Firebase.auth
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        val dbRef: DatabaseReference = FirebaseDatabase.getInstance().getReference()
        userMail.text = userEmail
        val firedb = Firebase.firestore


        val current = auth.currentUser
        userLogin.text = current?.displayName.toString()
        userMail.text = current?.email.toString()
        Log.i(TAG, "User credentials: ${current?.displayName}, ${current?.email}")


//        if (current != null) {
//            userLogin.text = current.login
//        }
//        else{
//            userLogin.text = "AAAAAAAAAAAAAAAAAAAA"
//        }
//
//        if(current!=null){
//            val imageID = resources.getIdentifier(current.avatar, "drawable", packageName)
//            avatar.setImageResource(imageID)
//        }
//        else{
//            val imageID = resources.getIdentifier("empty_avatar", "drawable", packageName)
//            avatar.setImageResource(imageID)
//        }


        val logoutButton: TextView = findViewById(R.id.buttonLogout)
        logoutButton.setOnClickListener {
//            sessionManager.clearLoginState()
//            val intent = Intent(this, AuthActivity::class.java)
//            startActivity(intent)
//            finish()

            Log.i(TAG, "Logout")
            auth.signOut()
            val logoutIntent = Intent(this, AuthActivity::class.java)
            logoutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(logoutIntent)
            finish()
        }

        val backButton: ImageView = findViewById(R.id.backButton)
        backButton.setOnClickListener {

            val intent = Intent(this, Home::class.java)
            startActivity(intent)
        }

        val editTop: ImageView = findViewById(R.id.editTop)
        editTop.setOnClickListener {
            val intent = Intent(this, ProfileEdit::class.java)
            startActivity(intent)
        }

        val myRestButton: Button = findViewById(R.id.myRest)
        if (current != null) {
            firedb.collection("users").whereEqualTo("email", current.email).get()
                .addOnSuccessListener { users ->
                    for (user in users) {
                        val restOwner = user.getLong("rest_id")?.toInt() ?: -1
                        if (restOwner != 1) {
                            myRestButton.visibility = View.GONE
                        } else {
                            myRestButton.visibility = View.VISIBLE
                        }
                    }

                }
        }

        myRestButton.setOnClickListener {
            val intent = Intent(this@Profile, MyRest::class.java)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()

        val userMail: TextView = findViewById(R.id.userMail)
        val userLogin: TextView = findViewById(R.id.userLogin)
        val sessionManager = SessionManager(this)
        val fireDb = Firebase.firestore
        val storage = Firebase.storage
        auth = Firebase.auth

        val userEmail = sessionManager.getUserEmail()
        userMail.text = userEmail

        val current = auth.currentUser
        userLogin.text = current?.displayName.toString()
        userMail.text = current?.email.toString()
        Log.i(TAG, "User credentials: ${current?.displayName}, ${current?.email}")


        val doc = fireDb.collection("users").whereEqualTo("email", current?.email).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) Log.d(TAG, "") else {
                    for (document in documents) {
                        val docId = document.id
                        Log.d("TAG", "Document ID: $docId")
                    }
                }
            }

    }

}