package com.example.foodresq.classes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast
import com.example.foodresq.adaptersEtc.ProductAdapter
import com.example.foodresq.views.Home
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DbHelper(val context: Context, val factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, "app", factory, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL("CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, login TEXT, email TEXT, password TEXT, avatar TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS food (id INT PRIMARY KEY, name TEXT, description TEXT, image TEXT, price INT, rest_id INT, user_id INT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS rests (id INT PRIMARY KEY, name TEXT, description TEXT, image TEXT, position INT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS cart (nomer INT PRIMARY KEY, user_id INT, position_id INT)")
        val firedb = Firebase.firestore
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS food")
        db.execSQL("DROP TABLE IF EXISTS rests")
        db.execSQL("DROP TABLE IF EXISTS cart")
        onCreate(db)
    }

    fun getUserByEmail(email: String): User? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE email = '$email'", null)

        if (cursor.moveToFirst()) {

            val login = cursor.getString(cursor.getColumnIndexOrThrow("login"))
            val avatar = cursor.getString(cursor.getColumnIndexOrThrow("avatar"))
            val password = cursor.getString(cursor.getColumnIndexOrThrow("password"))
            cursor.close()
            db.close()
            return User("", login, avatar, password)
        } else {
            cursor.close()
            db.close()
            return null
        }
    }

    fun addPosition(name: String, desc: String, image: String, price: Int) {
        val firedb = Firebase.firestore
        val auth = Firebase.auth
        val current = auth.currentUser

        val countersRef = firedb.collection("counters").document("counter_pos")
        val positionsRef = firedb.collection("positions")

        firedb.runTransaction { transaction ->
            val snapshot = transaction.get(countersRef)
            val currentId = snapshot.getLong("current_id") ?: 0

            val newId = currentId + 1

            val newPosition = hashMapOf(
                "id" to newId,
                "name" to name,
                "desc" to desc,
                "ava" to image,
                "price" to price,
                "rest_id" to current?.uid,
                "leftovers" to 0,
            )

            transaction.set(positionsRef.document(), newPosition)
            transaction.update(countersRef, "current_id", newId)

            null
        }
    }

    fun getRest(id: Int): Restaurant? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM rests WHERE id = '$id'", null)

        if (cursor.moveToFirst()) {
            val restId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
            val logo = cursor.getString(cursor.getColumnIndexOrThrow("image"))
            val position = cursor.getInt(cursor.getColumnIndexOrThrow("position"))

            cursor.close()
            db.close()

            return Restaurant("", restId, name, description, logo, position)
        } else {
            cursor.close()
            db.close()
            return null
        }
    }
}

