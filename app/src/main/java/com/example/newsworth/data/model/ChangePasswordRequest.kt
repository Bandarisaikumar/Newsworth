package com.example.newsworth.data.model

data class ChangePasswordRequest(
    val user_id: Int,
    val old_password: String,
    val new_password: String,
    val confirm_password: String
)
