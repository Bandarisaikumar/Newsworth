package com.example.newsworth.repository

import com.example.newsworth.data.api.ApiService
import com.example.newsworth.data.model.ContentUploadResponse
import com.example.newsworth.data.model.MetadataRequest
import com.example.newsworth.data.model.MetadataResponse
import com.example.newsworth.data.model.UploadedContentResponse
import okhttp3.RequestBody
import retrofit2.Response
import java.io.IOException
import javax.net.ssl.SSLHandshakeException
import java.net.SocketException
import javax.inject.Inject

class NewsWorthCreatorRepository @Inject constructor(private val apiService: ApiService) {

    suspend fun uploadMetadata(metadata: MetadataRequest): Response<MetadataResponse> {
        return apiService.insertMetadata(metadata)
    }

    suspend fun uploadContent(
        userId: Int,
        contentId: Int,
        title: String,
        mediaType: String,
        description: String,
        price: Float,
        discount: Int,
        filebase64_file: RequestBody,
        tags: RequestBody
    ): Response<ContentUploadResponse> {
        try {
            return apiService.uploadContent(
                userId,
                contentId,
                title,
                mediaType,
                description,
                price,
                discount,
                filebase64_file,
                tags
            )
        } catch (e: IOException) {
            if (e is SSLHandshakeException || e is SocketException) {
                throw SSLHandshakeException("SSL/Socket Error: ${e.message}")
            } else {
                throw IOException("Network Error: ${e.message}")
            }
        }
    }

    suspend fun fetchUploadedContent(userId: Int): Response<UploadedContentResponse> {
        return apiService.getUploadedContent(userId)
    }
}