package com.example.foodresq.adaptersEtc

import com.example.foodresq.classes.User
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DB {

    val fireDb = Firebase.firestore

    fun getUserByFactId(factId: String): User{
        val userList = mutableListOf<User>()
        fireDb.collection("users").document(factId).get().addOnSuccessListener {userDoc->
            val login = userDoc.getString("login")?: ""
            val email = userDoc.getString("email")?: ""
            val password = userDoc.getString("password")?: ""
            val ava = userDoc.getString("avatar")?: "empty_avatar"
            val restId = userDoc.getLong("rest_id")?.toInt() ?: -1

            userList.add(User(
                factId, login, email, password, ava, restId
            ))
        }
        return userList[0]
    }
}