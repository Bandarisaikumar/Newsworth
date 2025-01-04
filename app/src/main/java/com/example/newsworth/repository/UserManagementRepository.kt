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
                val response = apiService.login(request).execute() // Retrofit synchronous call
                if (response.isSuccessful) {
                    response.body() // Extract the body if the response is successful

                } else {
                    null // Handle failure case
                }
            } catch (e: Exception) {
                // Log or handle the exception as required
                null
            }
        }
    }
//
//    suspend fun getAccessToken(request: RequestBody): TokenResponse? {
//        return withContext(Dispatchers.IO) {
//            try {
//                val response = apiService.getAccessToken(request).execute()
//                if (response.isSuccessful) {
//                    response.body() // Return the token response if successful
//                } else {
//                    // Return a TokenResponse with the error message if the request fails
//                    TokenResponse(detail = "Incorrect email/mobile number or password")
//                }
//            } catch (e: Exception) {
//                // Handle the exception and return a TokenResponse with a generic error message
//                TokenResponse(detail = "An error occurred. Please try again.")
//            }
//        }
//    }
suspend fun getAccessToken(request: RequestBody): TokenResponse? {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAccessToken(request).execute()
            if (response.isSuccessful) {
                response.body() // Return the token response if successful
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorDetail(errorBody)
                TokenResponse(detail = errorMessage ?: "An unexpected error occurred.")
            }
        } catch (e: Exception) {
            // Handle the exception and return a TokenResponse with a generic error message
            TokenResponse(detail = "An error occurred. Please try again.")
        }
    }
}

    // Helper function to parse the error response and extract the detail message
    private fun parseErrorDetail(errorBody: String?): String? {
        return try {
            if (!errorBody.isNullOrEmpty()) {
                val jsonObject = JSONObject(errorBody)
                jsonObject.optString("detail") // Extract the "detail" field
            } else {
                null
            }
        } catch (e: JSONException) {
            null // Return null if parsing fails
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
                val response = apiService.forgotPassword(request) // Retrofit call
                if (response.isSuccessful) {
                    response.body() // Return the body if successful
                } else {
                    null // Handle failure case
                }
            } catch (e: Exception) {
                // Log or handle the exception
                null
            }
        }
    }

    suspend fun resetPassword(request: ResetPasswordRequest) = apiService.resetPassword(request)

}
