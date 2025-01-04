package com.example.newsworth.data.model

data class LoginRequest(
    val login_option: String,
    val platform: String,
    val email_or_mobile_or_user_id: String,
    val password: String
)
