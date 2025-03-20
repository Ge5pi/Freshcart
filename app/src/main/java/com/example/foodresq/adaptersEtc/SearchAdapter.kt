package com.example.foodresq.adaptersEtc

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.classes.Product

class SearchAdapter(private var foods: List<Product>, private val context: Context) :
    RecyclerView.Adapter<SearchAdapter.MyViewHolder>() {

    private var mListener: OnItemClickListener? = null
    private var cListener: AddToCartClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(id: Int)
    }

    interface AddToCartClickListener {
        fun addToCart(id: Int, toCartButton: Button)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    fun setOnAddToCartClickListener(listener: AddToCartClickListener) {
        cListener = listener
    }


    class MyViewHolder(view: View, listener: OnItemClickListener?, listenerCart: AddToCartClickListener?) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.img)
        val name: TextView = view.findViewById(R.id.name)
        val price: TextView = view.findViewById(R.id.price)
        val toCartButton: Button = view.findViewById(R.id.toCart)
        val leftovers: TextView = view.findViewById(R.id.leftovers)
        var id: Int = 0

        init {
            view.setOnClickListener {
                listener?.onItemClick(id)
            }

            toCartButton.setOnClickListener {
                listenerCart?.addToCart(id, toCartButton)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.product_in_search, parent, false)
        return MyViewHolder(view, mListener, cListener)
    }

    override fun getItemCount(): Int {
        return foods.count()
    }

    fun updateList(newList: List<Product>) {
        foods = newList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val product = foods[position]
        if (product.leftovers == 0) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        } else {
            holder.leftovers.text = "Остаток: ${product.leftovers}"
            holder.name.text = product.name
            holder.price.text =  "₸${product.price}"
            holder.id = product.id
            val initImg = product.image

            Log.d("ProductAdapter", "initImg: $initImg")

            Glide.with(context)
                .load(initImg)
                .into(holder.img)
        }
    }
}
