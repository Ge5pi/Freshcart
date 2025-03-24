package com.example.foodresq.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.foodresq.R
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
import java.security.MessageDigest

class AuthActivity : Activity() {

    private companion object {
        private const val RC_GOOGLE_SIGN_IN = 4926
        private const val TAG = "AuthActivity"
    }

    private lateinit var auth: FirebaseAuth

    // Функция для проверки доступности сети
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

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
            if (isNetworkAvailable()) {
                val signIntIntent = client.signInIntent
                startActivityForResult(signIntIntent, RC_GOOGLE_SIGN_IN)
            } else {
                Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "No internet connection available for Google Sign-In")
            }
        }

        auth = Firebase.auth

        hint.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        buttonAuth.setOnClickListener {
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "No internet connection available for authentication")
                return@setOnClickListener
            }

            val userEmail = email.text.toString().trim()
            val userPassword = password.text.trim().hashCode().toString()

            if (userEmail.isEmpty() || password.text.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Показываем пользователю, что идет процесс
            Toast.makeText(this, "Выполняется вход...", Toast.LENGTH_SHORT).show()

            auth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success")
                        val user = auth.currentUser
                        updateUI(user)
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            this,
                            "Ошибка входа: ${task.exception?.localizedMessage ?: "Неизвестная ошибка"}",
                            Toast.LENGTH_LONG
                        ).show()
                        updateUI(null)
                    }
                }
        }

        backButton.setOnClickListener {
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
            Log.w(TAG, "User is null, not going to navigate")
            return
        }

        startActivity(Intent(this, Home::class.java))
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "FirebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Ошибка входа через Google: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "No internet connection available for Google authentication")
            return
        }

        // Показываем пользователю, что идет процесс
        Toast.makeText(this, "Выполняется вход через Google...", Toast.LENGTH_SHORT).show()

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val fireDb = Firebase.firestore
                    val usersCol = fireDb.collection("users")

                    fireDb.collection("users")
                        .whereEqualTo("email", auth.currentUser?.email.toString())
                        .get()
                        .addOnSuccessListener { users ->
                            if (users.isEmpty) {
                                Log.i(TAG, "No user found, creating new user")
                                val userData = hashMapOf(
                                    "login" to (auth.currentUser?.displayName.toString()),
                                    "email" to auth.currentUser?.email.toString(),
                                    "password" to auth.currentUser?.uid.toString(),
                                    "rest_id" to -1,
                                    "avatar" to "",
                                    "cart" to listOf<Int>()
                                )

                                usersCol.add(userData)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "User document added successfully")
                                        updateUI(auth.currentUser)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error adding user document", e)
                                        Toast.makeText(this, "Ошибка создания профиля: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Log.i(TAG, "User already registered")
                                updateUI(auth.currentUser)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error checking if user exists", e)
                            Toast.makeText(this, "Ошибка проверки пользователя: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(
                        this,
                        "Ошибка аутентификации: ${task.exception?.localizedMessage ?: "Неизвестная ошибка"}",
                        Toast.LENGTH_LONG
                    ).show()
                    updateUI(null)
                }
            }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        return digest.toHexString()
    }
}