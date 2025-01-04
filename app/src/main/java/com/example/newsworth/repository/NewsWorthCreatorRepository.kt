package com.example.newsworth.repository

import com.example.newsworth.data.api.ApiService
import com.example.newsworth.data.model.ContentUploadResponse
import com.example.newsworth.data.model.MetadataRequest
import com.example.newsworth.data.model.MetadataResponse
import com.example.newsworth.data.model.UploadedContentResponse
import okhttp3.RequestBody
import retrofit2.Response

class NewsWorthCreatorRepository(private val apiService: ApiService) {

    suspend fun uploadMetadata(metadata: MetadataRequest): Response<MetadataResponse> {
        return apiService.insertMetadata(metadata)
    }

    suspend fun uploadContent(userId: Int, contentId: Int, title: String, mediaType: String, description: String, price: Float, discount: Int,
                              filebase64_file: RequestBody, tags: RequestBody): Response<ContentUploadResponse> {
        return apiService.uploadContent(userId,contentId, title, mediaType,description, price, discount, filebase64_file, tags)
    }

    // Fetch uploaded content
    suspend fun fetchUploadedContent(userId: Int): Response<UploadedContentResponse> {
        return apiService.getUploadedContent(userId)
    }
}
