package com.example.newsworth.data.model

data class LocationResponse(
    val response: String,
    val response_message: String,
    val data: List<LocationDetail>
)