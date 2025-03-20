// com/example/foodresq/adaptersEtc/FeedbackAdapter.kt
package com.example.foodresq.adaptersEtc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodresq.R
import com.example.foodresq.classes.Review
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private val fireDb = Firebase.firestore

class FeedbackAdapter(private val reviews: List<Review>, private val context: Context) :
    RecyclerView.Adapter<FeedbackAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.userName)
        val reviewText: TextView = itemView.findViewById(R.id.reviewText)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.review_in_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]
        fireDb.collection("users").document(review.userId).get().addOnSuccessListener {
            doc -> holder.userName.text = doc.getString("login").toString()
        }.addOnFailureListener {
            holder.userName.text = "Unknown User"
        }
        holder.reviewText.text = review.text
        holder.ratingBar.rating = review.rating
    }

    override fun getItemCount(): Int = reviews.size
}