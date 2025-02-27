package com.example.newsworth.repository

import com.example.newsworth.data.api.ApiService
import com.example.newsworth.data.model.ForgotPasswordRequest
import com.example.newsworth.data.model.ForgotPasswordResponse
import com.example.newsworth.data.model.LoginRequest
import com.example.newsworth.data.model.LoginResponse
import com.example.newsworth.data.model.LogoutResponse
import com.example.newsworth.data.model.RegistrationModels
import com.example.newsworth.data.model.ResetPasswordRequest
import com.example.newsworth.data.model.TokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response

class UserManagementRepository(private val apiService: ApiService) {

    suspend fun login(request: LoginRequest): LoginResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(request).execute()
                if (response.isSuccessful) {
                    response.body()

                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getAccessToken(request: RequestBody): TokenResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAccessToken(request).execute()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = parseErrorDetail(errorBody)
                    TokenResponse(detail = errorMessage ?: "An unexpected error occurred.")
                }
            } catch (e: Exception) {
                TokenResponse(detail = "An error occurred. Please try again.")
            }
        }
    }

    private fun parseErrorDetail(errorBody: String?): String? {
        return try {
            if (!errorBody.isNullOrEmpty()) {
                val jsonObject = JSONObject(errorBody)
                jsonObject.optString("detail")
            } else {
                null
            }
        } catch (e: JSONException) {
            null
        }
    }


    suspend fun registerUser(request: RegistrationModels.RegistrationRequest): Response<RegistrationModels.RegistrationResponse> {
        return apiService.registerUser(request)
    }

    suspend fun sendOtp(request: RegistrationModels.SendOtpRequest): Response<RegistrationModels.SendOtpResponse> {
        return apiService.sendOtp(request)
    }

    suspend fun verifySignup(request: RegistrationModels.SignupVerificationRequest): Response<RegistrationModels.SignupVerificationResponse> {
        return apiService.verifySignup(request)
    }

    suspend fun logoutUser(userId: Int): Response<LogoutResponse> {
        return apiService.logoutUser(mapOf("user_id" to userId))
    }

    suspend fun forgotPassword(request: ForgotPasswordRequest): ForgotPasswordResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.forgotPassword(request)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun resetPassword(request: ResetPasswordRequest) = apiService.resetPassword(request)

}
