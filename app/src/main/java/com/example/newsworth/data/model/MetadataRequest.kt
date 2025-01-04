package com.example.newsworth.data.model

data class MetadataRequest(
    val user_id: Int,
    val gps_location: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val mobile_type: String,
    val mobile_os: String,
    val brand: String,
    val resolution: String,
    val uploaded_time: String,
    val incident_time: String
)

