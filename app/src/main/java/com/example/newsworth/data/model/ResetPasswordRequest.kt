package com.example.newsworth.data.model


data class ResetPasswordRequest(
    val user_id: Int,
    val otp: String,
    val new_password: String,
    val confirm_new_password: String
)


