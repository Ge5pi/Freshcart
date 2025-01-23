package com.example.foodresq.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.foodresq.R
import com.example.foodresq.adaptersEtc.SessionManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MainActivity : ComponentActivity() {

    private companion object {
        private const val RC_GOOGLE_SIGN_IN = 4926
        private const val TAG = "MainActivity"

    }

    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SessionManager
        val sessionManager = SessionManager(this)

        // Check login state
        if (sessionManager.isLoggedIn()) {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val username = findViewById<EditText>(R.id.name)
        val userEmail = findViewById<EditText>(R.id.email)
        val userPassword = findViewById<EditText>(R.id.password)
        val buttonReg = findViewById<Button>(R.id.buttonReg)
        val hint = findViewById<TextView>(R.id.hint4auth)
        val backButton = findViewById<ImageView>(R.id.backButton)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        val regGoogle =
            findViewById<com.google.android.gms.common.SignInButton>(R.id.buttonRegGoogle)
        regGoogle.setOnClickListener {
            val signIntIntent = client.signInIntent
            startActivityForResult(signIntIntent, RC_GOOGLE_SIGN_IN)
        }
        val fireDb = Firebase.firestore
        val usersCol = fireDb.collection("users")
        auth = Firebase.auth

        buttonReg.setOnClickListener {
            val login = username.text.toString().trim()
            val email = userEmail.text.toString().trim()
            val password = userPassword.text.toString().trim()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {

                        val userData = hashMapOf(
                            "login" to login,
                            "email" to email,
                            "password" to password,
                            "rest_id" to -1,
                            "avatar" to "",
                            "cart" to listOf<Int>()
                        )
                        usersCol.add(userData).addOnSuccessListener { documentReference ->
                            Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                        }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error adding document", e)
                            }

                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val profileUpdate =
                            UserProfileChangeRequest.Builder().setDisplayName(login).build()
                        currentUser?.updateProfile(profileUpdate)
                        updateUI(user)
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            this,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        updateUI(null)
                    }
                }

        }


        hint.setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }

        backButton.setOnClickListener {
            // Handle back button functionality
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user == null) {
            Log.w(TAG, "User is null, not going to navigate")
            return
        }

        startActivity(Intent(this, Home::class.java))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "FirebaseAuthWithGoogle:" + account.id)

                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        //getting user credentials with the help of AuthCredential method and also passing user Token Id.

        val credential = GoogleAuthProvider.getCredential(idToken, null)


        //trying to sign in user using signInWithCredential and passing above credentials of user.
        auth.signInWithCredential(credential)
            .addOnCompleteListener(
                this,
                OnCompleteListener<AuthResult?> { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithCredential:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        Log.d(TAG, "signInWithCredential:success", task.exception)
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT)
                        updateUI(null)
                    }
                })
    }
}
