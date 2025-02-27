package com.example.newsworth.data.model

data class EditProfileRequest(
    val user_id: Int?,
    val first_name: String,
    val middle_name: String?,
    val last_name: String,
    val gender: String?,
    val user_email: String?,
    val user_phone_number: String,
    val user_type: String,
    val date_of_birth: String?,
    val user_address_line_1: String?,
    val user_address_line_2: String?,
    val pin_code: String,
    val location_name: String?,
    val district_name: String?,
    val state_name: String?,
    val country_name: String?
)

