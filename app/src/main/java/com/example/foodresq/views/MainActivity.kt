package com.example.foodresq.views

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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private companion object {
        private const val RC_GOOGLE_SIGN_IN = 4926
        private const val TAG = "MainActivity"
    }

    private lateinit var auth: FirebaseAuth

    // Добавляем функцию проверки доступности сети
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

        val sessionManager = SessionManager(this)

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
            // Проверяем наличие интернет-соединения перед выполнением входа через Google
            if (isNetworkAvailable()) {
                val signIntIntent = client.signInIntent
                startActivityForResult(signIntIntent, RC_GOOGLE_SIGN_IN)
            } else {
                Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show()
            }
        }
        val fireDb = Firebase.firestore
        val usersCol = fireDb.collection("users")
        auth = Firebase.auth

        buttonReg.setOnClickListener {
            // Проверяем наличие интернет-соединения перед регистрацией
            if (isNetworkAvailable()) {
                val login = username.text.toString().trim()
                val email = userEmail.text.toString().trim()
                var password = userPassword.text.trim().hashCode().toString()

                // Проверка на пустые поля
                if (login.isEmpty() || email.isEmpty() || userPassword.text.isEmpty()) {
                    Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

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
                                "Ошибка регистрации: ${task.exception?.message}",
                                Toast.LENGTH_SHORT,
                            ).show()
                            updateUI(null)
                        }
                    }
            } else {
                Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show()
            }
        }

        hint.setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }

        backButton.setOnClickListener {
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
                Toast.makeText(this, "Ошибка входа через Google: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        // Проверяем наличие интернет-соединения перед аутентификацией
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(
                this,
                OnCompleteListener<AuthResult?> { task ->
                    if (task.isSuccessful) {
                        val fireDb = Firebase.firestore
                        val usersCol = fireDb.collection("users")
                        fireDb.collection("users")
                            .whereEqualTo("email", auth.currentUser?.email.toString()).get()
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

                        Log.d(TAG, "signInWithCredential:success")
                        val user = auth.currentUser
                        updateUI(user)

                    } else {
                        Log.d(TAG, "signInWithCredential:failure", task.exception)
                        Toast.makeText(this, "Ошибка аутентификации: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                })
    }
}