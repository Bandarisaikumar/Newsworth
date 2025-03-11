package com.example.newsworth.data.model

data class ImageModel(
    val content_title :String ?= null,
    val uploaded_by: String,
    val content_description: String ? = null,
    val content_categories: String? = null,
    val gps_location: String,
    val age_in_days: String,
    val price: String,
    val discount: String,
    val Image_link: String?=null,
    val Audio_link: String?=null,
    val Video_link:String?=null,
)