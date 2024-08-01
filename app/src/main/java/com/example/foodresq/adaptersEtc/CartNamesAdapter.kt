package com.example.foodresq.adaptersEtc

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foodresq.R
import com.example.foodresq.classes.Restaurant

class CartNamesAdapter(private val rests: List<Restaurant>, private val context: Context): RecyclerView.Adapter<CartNamesAdapter.MyViewHolder>() {

    private var nameListener: OnNameClickListener? = null

    interface OnNameClickListener{
        fun onNameClick(id: Int)
    }

    fun setOnNameClickListener(listener: OnNameClickListener){
        nameListener = listener
    }

    class MyViewHolder(view: View, listener: OnNameClickListener?): RecyclerView.ViewHolder(view){
        val cartNameText: TextView = view.findViewById(R.id.cartNameRest)
        var id=0

        init{
            view.setOnClickListener {
                listener?.onNameClick(id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.name_in_cart, parent, false)
        return MyViewHolder(view, nameListener)
    }

    override fun getItemCount(): Int {
        return rests.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val rest = rests[position]
        Log.i("CartNamesAdapter", "rest: $rest")
        holder.cartNameText.text = rest.name
        holder.id = rest.id
    }

}