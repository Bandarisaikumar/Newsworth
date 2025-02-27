package com.example.newsworth.data.model

data class LoginResponse(
    val response: String,
    val response_message: String,
    val data: List<UserData>
)


