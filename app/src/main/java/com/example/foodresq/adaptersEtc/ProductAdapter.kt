package com.example.foodresq.adaptersEtc

import android.annotation.SuppressLint
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

class ProductAdapter(private var foods: List<Product>, private val context: Context) :
    RecyclerView.Adapter<ProductAdapter.MyViewHolder>() {

    private var mListener: OnItemClickListener? = null
    private var cListener: AddToCartClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(id: Int)
    }

    interface AddToCartClickListener {
        fun addToCart(id: Int, toCartButton: Button, inCartButton: Button)
    }


    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    fun setOnAddToCartClickListener(listener: AddToCartClickListener) {
        cListener = listener
    }

    fun updateList(newList: List<Product>) {
        foods = newList
        notifyDataSetChanged()
    }

    class MyViewHolder(
        view: View,
        listener: OnItemClickListener?,
        listenerCart: AddToCartClickListener?
    ) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.productPic)
        val name: TextView = view.findViewById(R.id.productName)
        val price: TextView = view.findViewById(R.id.productPrice)
        val toCartButton: Button = view.findViewById(R.id.toCart)
        val leftovers: TextView = view.findViewById(R.id.leftovers)
        var id: Int = 0

        init {
            view.setOnClickListener {
                listener?.onItemClick(id)
            }

            toCartButton.setOnClickListener {
                listenerCart?.addToCart(id, toCartButton, view.findViewById(R.id.toCart))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.product_in_list, parent, false)
        return MyViewHolder(view, mListener, cListener)
    }

    override fun getItemCount(): Int {
        return foods.count()
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val product = foods[position]
        if (product.leftovers == 0) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        } else {
            holder.leftovers.text = "Остаток: ${product.leftovers}"
            holder.name.text = product.name
            holder.price.text = product.price.toString() + "₸"
            holder.id = product.id
            val initImg = product.image

            Log.d("ProductAdapter", "initImg: $initImg")

            // Use Glide to load the image
            Glide.with(context)
                .load(initImg)
                .into(holder.img)
        }
    }
}
