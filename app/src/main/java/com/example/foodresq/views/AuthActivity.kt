package com.example.foodresq.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.classes.DbHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AuthActivity : Activity() {

    private companion object {
        private const val RC_GOOGLE_SIGN_IN = 4926
        private const val TAG = "AuthActivity"

    }

    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val email = findViewById<EditText>(R.id.emailAuth)
        val password = findViewById<EditText>(R.id.passwordAuth)
        val buttonAuth = findViewById<Button>(R.id.buttonAuth)
        val hint = findViewById<TextView>(R.id.hint4reg)
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
            startActivityForResult(signIntIntent, AuthActivity.RC_GOOGLE_SIGN_IN)
        }
        auth = Firebase.auth

        hint.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }



        buttonAuth.setOnClickListener {
            val userEmail = email.text.toString().trim()
            val userPassword = password.text.toString().trim()
//            if (userPassword == "" || userEmail == "") {
//                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_LONG).show()
//            } else {
//                val isAuth = db.getUser(userEmail, userPassword)
//                if (isAuth) {
//                    Toast.makeText(this, "Вы вошли в аккаунт", Toast.LENGTH_LONG).show()
//                    email.text.clear()
//                    password.text.clear()
//
//                    // Save login state and user email
//                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
//                    with(sharedPreferences.edit()) {
//                        putBoolean("is_logged_in", true)
//                        putString("user_email", userEmail)
//                        apply()
//                    }
//
//                    val intent = Intent(this, Home::class.java)
//                    startActivity(intent)
//                    finish()
//                } else {
//                    Toast.makeText(this, "Неверное имя пользователя или пароль", Toast.LENGTH_LONG).show()
//                }
//            }
            auth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        updateUI(null)
                    }
                }
        }

        backButton.setOnClickListener {
            // Handle back button functionality
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user == null) {
            Log.w(AuthActivity.TAG, "User is null, not going to navigate")
            return
        }

        startActivity(Intent(this, Home::class.java))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AuthActivity.RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(AuthActivity.TAG, "FirebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Log.w(AuthActivity.TAG, "Google sign in failed", e)
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
                        val fireDb = Firebase.firestore
                        val usersCol = fireDb.collection("users")
                        fireDb.collection("users").whereEqualTo("email", auth.currentUser?.email.toString()).get()
                            .addOnSuccessListener { users ->
                                if (users.isEmpty) {
                                    Log.i(TAG, "No user found")
                                    val userData = hashMapOf(
                                        "login" to (auth.currentUser?.displayName.toString()),
                                        "email" to auth.currentUser?.email.toString(),
                                        "password" to auth.currentUser?.uid.toString(),
                                        "rest_id" to -1,
                                        "avatar" to "",
                                        "cart" to listOf<Int>()
                                    )

                                    usersCol.add(userData)
                                } else {
                                    Log.i(TAG, "User already registered")
                                }
                            }

                        Log.d(AuthActivity.TAG, "signInWithCredential:success")
                        val user = auth.currentUser
                        updateUI(user)

                    } else {
                        Log.d(AuthActivity.TAG, "signInWithCredential:success", task.exception)
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT)
                        updateUI(null)
                    }
                })
    }
}
