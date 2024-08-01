package com.example.foodresq.classes

data class Restaurant(
    val factId: String,
    val id: Int,
    val name: String,
    val desc: String,
    val logo: String,
    val position: Int = 0
)