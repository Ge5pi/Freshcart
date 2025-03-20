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

        holder.userName.text = review.userId
        holder.reviewText.text = review.text

        // Set the rating
        holder.ratingBar.rating = review.rating
    }

    override fun getItemCount(): Int = reviews.size
}