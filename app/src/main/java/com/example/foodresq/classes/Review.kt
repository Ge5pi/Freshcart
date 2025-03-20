package com.example.foodresq.classes

data class Review(
    val userId: String,
    val text: String,
    val rating: Float,
    val restaurantId: String
)