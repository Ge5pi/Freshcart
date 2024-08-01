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

class FeedbackAdapter(private val reviews: List<Review>, private val context: Context): RecyclerView.Adapter<FeedbackAdapter.MyViewHolder>() {

    class MyViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val username: TextView = view.findViewById(R.id.username)
        val reviewText: TextView = view.findViewById(R.id.reviewText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.review_in_list, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return reviews.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val review = reviews[position]
        holder.reviewText.text = review.text
        holder.ratingBar.rating = review.rating
        holder.ratingBar.isEnabled = false
        val fireDb = Firebase.firestore
        fireDb.collection("users").document(review.userId).get()
            .addOnSuccessListener { doc ->
                holder.username.text = doc.getString("login") ?: "Unknown User"
            }
            .addOnFailureListener {
                holder.username.text = "Unknown User"
            }
    }
}
