package com.example.foodresq.classes

data class Product(
    val id: Int,
    val fact_id: String,
    val name: String,
    val desc: String,
    val image: String,
    val price: Int,
    val restId: Int,
    val leftovers: Int = 0
)