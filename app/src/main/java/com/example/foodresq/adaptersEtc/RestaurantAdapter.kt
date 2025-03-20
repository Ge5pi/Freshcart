package com.example.foodresq.adaptersEtc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodresq.R
import com.example.foodresq.classes.Restaurant

class RestaurantAdapter(val rests: List<Restaurant>, val context: Context) :
    RecyclerView.Adapter<RestaurantAdapter.MyViewHolder>() {

    private var mListener: onItemClickListener? = null

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: onItemClickListener) {
        mListener = listener
    }

    class MyViewHolder(view: View, listener: onItemClickListener?) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.restName)
        val desc: TextView = view.findViewById(R.id.desc)
        val img: ImageView = view.findViewById(R.id.restImg)

        init {
            view.setOnClickListener {
                listener?.onItemClick(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rest_in_list, parent, false)
        return MyViewHolder(view, mListener)
    }

    override fun getItemCount(): Int {
        return rests.count()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.name.text = rests[position].name
        holder.desc.text = rests[position].desc
        Glide.with(context)
            .load(rests[position].logo)
            .into(holder.img)
    }
}
