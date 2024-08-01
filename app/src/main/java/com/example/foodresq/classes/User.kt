package com.example.foodresq.classes

class User(val id: String, var login: String, val email: String, val password: String, val avatar: String = "empty_avatar", val is_owner: Int = 0, val rest_id: Int = 1)