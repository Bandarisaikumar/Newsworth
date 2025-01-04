package com.example.newsworth.data.model

import com.google.gson.annotations.SerializedName

data class MetadataResponse(
    val response: String,
    val response_message: String,
    @SerializedName("Content_id")
    val content_id: Int
)

