package com.example.newsworth.repository

import com.example.newsworth.data.api.ApiService
import com.example.newsworth.data.model.ChangePasswordRequest
import com.example.newsworth.data.model.ChangePasswordResponse
import com.example.newsworth.data.model.EditProfileRequest
import com.example.newsworth.data.model.EditProfileResponse
import com.example.newsworth.data.model.GetProfileResponse
import com.example.newsworth.data.model.ImageLinkResponse
import com.example.newsworth.data.model.ImageUploadResponse
import com.example.newsworth.data.model.LocationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import retrofit2.Response
import java.io.IOException
import java.net.SocketException
import javax.net.ssl.SSLHandshakeException

class ProfileManagementRepository(private val apiService: ApiService) {

    suspend fun uploadProfileImage(userId: Int, file: MultipartBody.Part): Response<ImageUploadResponse> {
        return apiService.uploadProfileImage(userId, file)
    }
    suspend fun getProfileDetails(userId: Int): Response<GetProfileResponse> {
        try {
            return apiService.getProfileDetails(userId)
        } catch (e: IOException) {
            if (e is SSLHandshakeException || e is SocketException) {
                throw SSLHandshakeException("SSL/Socket Error: ${e.message}")
            } else {
                throw IOException("Network Error: ${e.message}")
            }
        }
    }

    suspend fun getImageLink(userId: String): Response<ImageLinkResponse> {
        try {
            return apiService.getImageLink(userId)
        } catch (e: IOException) {
            if (e is SSLHandshakeException || e is SocketException) {
                throw SSLHandshakeException("SSL/Socket Error: ${e.message}")
            } else {
                throw IOException("Network Error: ${e.message}")
            }
        }
    }

    suspend fun editProfileDetails(userId: String,profileRequest: EditProfileRequest): Response<EditProfileResponse> {
        return apiService.editProfileDetails(userId, profileRequest)
    }
    suspend fun changeUserPassword(request: ChangePasswordRequest): Response<ChangePasswordResponse> {
        return apiService.changeUserPassword(request)
    }

    suspend fun getLocationDetails(pincode: Int): LocationResponse? {
        return withContext(Dispatchers.IO) {
            val response = apiService.getLocationDetails(mapOf("pincode" to pincode))
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        }
    }
}
