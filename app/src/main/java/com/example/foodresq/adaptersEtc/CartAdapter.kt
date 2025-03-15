package com.example.foodresq.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.classes.Product
import com.example.foodresq.views.CartList.Companion.MAIN_ID

class CartAdapter(
    private val items: MutableList<Product>,
    private val context: Context
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    private var onDeleteClick: ((Int, Product) -> Unit)? = null

    fun setOnDeleteCartClickListener(listener: (Int, Product) -> Unit) {
        onDeleteClick = listener
    }

    fun updateItems(newItems: List<Product>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.cartPicProd)
        val nameText: TextView = view.findViewById(R.id.nameInCart)
        val priceText: TextView = view.findViewById(R.id.priceInCart)
        val quantityText: TextView = view.findViewById(R.id.quantity)
        val deleteButton: ImageView = view.findViewById(R.id.deleteCart)
        val containerView: View = view

        init {
            deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick?.invoke(position, items[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.product_in_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = items[position]

        with(holder) {
            nameText.text = product.name
            priceText.text = "₸${product.price}"
            quantityText.text = "Количество: ${product.leftovers}"

            Glide.with(context)
                .load(product.image)
                .into(productImage)

            Glide.with(context)
                .load("https://firebasestorage.googleapis.com/v0/b/foodresq-bc5d2.appspot.com/o/delete.png?alt=media&token=6942c1d2-097a-445a-b29a-d21e5875ba50")
                .into(deleteButton)

            if (MAIN_ID != product.restId) {
                containerView.visibility = View.GONE
                containerView.layoutParams = RecyclerView.LayoutParams(0, 0)
            } else {
                containerView.visibility = View.VISIBLE
                containerView.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }
    }

    override fun getItemCount(): Int = items.size
}