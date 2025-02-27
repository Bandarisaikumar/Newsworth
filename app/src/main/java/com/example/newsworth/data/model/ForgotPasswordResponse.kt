package com.example.newsworth.data.model

import com.google.gson.annotations.SerializedName

data class ForgotPasswordResponse(
    @SerializedName("response") val response: String,
    @SerializedName("response_message") val responseMessage: String,
    @SerializedName("data") val data: List<UserData2>
)