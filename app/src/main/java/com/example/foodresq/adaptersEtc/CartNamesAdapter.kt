package com.example.foodresq.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.foodresq.R
import com.example.foodresq.classes.Restaurant
import com.example.foodresq.views.CartList.Companion.MAIN_ID

class CartNamesAdapter(
    private val restaurants: MutableList<Restaurant>,
    private val context: Context
) : RecyclerView.Adapter<CartNamesAdapter.ViewHolder>() {

    private var onNameClick: ((Int) -> Unit)? = null

    fun setOnNameClickListener(listener: (Int) -> Unit) {
        onNameClick = listener
    }

    fun updateItems(newItems: List<Restaurant>) {
        restaurants.clear()
        restaurants.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.cartNameRest)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNameClick?.invoke(restaurants[position].id)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.name_in_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val restaurant = restaurants[position]

        with(holder) {
            nameText.text = restaurant.name

            if (MAIN_ID == restaurant.id) {
                nameText.setTextColor(ContextCompat.getColor(context, R.color.black))

            } else {
                nameText.setTextColor(ContextCompat.getColor(context, R.color.black))

            }
        }
    }

    override fun getItemCount(): Int = restaurants.size
}