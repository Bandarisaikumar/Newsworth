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

class ProfileManagementRepository(private val apiService: ApiService) {

    suspend fun uploadProfileImage(userId: Int, file: MultipartBody.Part): Response<ImageUploadResponse> {
        return apiService.uploadProfileImage(userId, file)
    }
    suspend fun getImageLink(userId: String): ImageLinkResponse? {
        return try {
            val response = apiService.getImageLink(userId)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    suspend fun getProfileDetails(userId: Int): Response<GetProfileResponse> {
        return apiService.getProfileDetails(userId)
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
