package com.example.foodresq.adaptersEtc

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.classes.Product
import com.example.foodresq.views.CartList.Companion.MAIN_ID

class CartAdapter(private val foods: List<Product>, private val context: Context) :
    RecyclerView.Adapter<CartAdapter.MyViewHolder>() {

    private var dListener: DeleteCartClickListener? = null
    private var totalPriceTextView: TextView? = null

    interface DeleteCartClickListener {
        fun onDeleteClick(id: Int, deleteCart: ImageView)
    }

    fun setOnDeleteCartClickListener(listener: DeleteCartClickListener) {
        dListener = listener
    }

    class MyViewHolder(view: View, listener: DeleteCartClickListener?) :
        RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.cartPicProd)
        val name: TextView = view.findViewById(R.id.nameInCart)
        val price: TextView = view.findViewById(R.id.priceInCart)
        val quantity: TextView = view.findViewById(R.id.quantity)
        val delete: ImageView = view.findViewById(R.id.deleteCart)
        var id = 0
        val view1 = view

        init {
            delete.setOnClickListener {
                listener?.onDeleteClick(id, delete)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.product_in_cart, parent, false)
        totalPriceTextView = (parent.context as ComponentActivity).findViewById(R.id.total)
        return MyViewHolder(view, dListener)
    }

    override fun getItemCount(): Int {
        return foods.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val product = foods[position]
        holder.name.text = product.name
        holder.price.text = "₸${product.price}"
        Glide.with(context)
            .load(product.image)
            .into(holder.img)
        holder.quantity.text = "Количество: " + product.leftovers.toString()
        holder.id = position

        Glide.with(context)
            .load("https://firebasestorage.googleapis.com/v0/b/foodresq-bc5d2.appspot.com/o/delete.png?alt=media&token=6942c1d2-097a-445a-b29a-d21e5875ba50")
            .into(holder.delete)

        if (MAIN_ID != product.restId) {
            holder.view1.visibility = View.GONE
            holder.view1.layoutParams = RecyclerView.LayoutParams(0, 0)
        } else {
            holder.view1.visibility = View.VISIBLE
            holder.view1.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Update the total price
        updateTotalPrice()
    }

     private fun updateTotalPrice() {
        val totalPrice = foods.filter { MAIN_ID == it.restId }.sumOf { it.price } * foods.filter { MAIN_ID == it.restId }.sumOf { it.leftovers }
        totalPriceTextView?.text = "₸$totalPrice"
    }
}
