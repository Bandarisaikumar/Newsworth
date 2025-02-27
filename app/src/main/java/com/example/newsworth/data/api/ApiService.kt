package com.example.newsworth.data.api

import com.example.newsworth.data.model.ChangePasswordRequest
import com.example.newsworth.data.model.ChangePasswordResponse
import com.example.newsworth.data.model.ContentUploadResponse
import com.example.newsworth.data.model.EditProfileRequest
import com.example.newsworth.data.model.EditProfileResponse
import com.example.newsworth.data.model.ForgotPasswordRequest
import com.example.newsworth.data.model.ForgotPasswordResponse
import com.example.newsworth.data.model.GetProfileResponse
import com.example.newsworth.data.model.ImageLinkResponse
import com.example.newsworth.data.model.ImageUploadResponse
import com.example.newsworth.data.model.LocationResponse
import com.example.newsworth.data.model.LoginRequest
import com.example.newsworth.data.model.LoginResponse
import com.example.newsworth.data.model.LogoutResponse
import com.example.newsworth.data.model.MetadataRequest
import com.example.newsworth.data.model.MetadataResponse
import com.example.newsworth.data.model.RegistrationModels
import com.example.newsworth.data.model.ResetPasswordRequest
import com.example.newsworth.data.model.ResetPasswordResponse
import com.example.newsworth.data.model.TokenResponse
import com.example.newsworth.data.model.UploadedContentResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {

    @POST("rgstr_dtls")
    suspend fun registerUser(@Body request: RegistrationModels.RegistrationRequest): Response<RegistrationModels.RegistrationResponse>

    @POST("Send_OTP")
    suspend fun sendOtp(@Body request: RegistrationModels.SendOtpRequest): Response<RegistrationModels.SendOtpResponse>

    @POST("sgnup_verification")
    suspend fun verifySignup(@Body request: RegistrationModels.SignupVerificationRequest): Response<RegistrationModels.SignupVerificationResponse>

    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("token")
    fun getAccessToken(@Body requestBody: RequestBody): Call<TokenResponse>

    @POST("insert-metadata")
    suspend fun insertMetadata(@Body metadata: MetadataRequest): Response<MetadataResponse>

    @Multipart
    @POST("content-upload")
    suspend fun uploadContent(
        @Query("user_id") userId: Int,
        @Query("content_id") contentId: Int,
        @Query("content_title") contentTitle: String,
        @Query("content_type") contentType: String,
        @Query("content_description") contentDescription: String,
        @Query("price") price: Float,
        @Query("discount") discount: Int,
        @Part("base64_file") base64Image: RequestBody,
        @Part("tags") tags: RequestBody
    ): Response<ContentUploadResponse>

    @POST("uploaded_content")
    suspend fun getUploadedContent(@Query("user_id") userId: Int): Response<UploadedContentResponse>

    @POST("usr_logout/")
    suspend fun logoutUser(@Body userId: Map<String, Int>): Response<LogoutResponse>

    @Multipart
    @POST("upload-image")
    suspend fun uploadProfileImage(
        @Query("user_id") userId: Int,
        @Part file: MultipartBody.Part
    ): Response<ImageUploadResponse>

    @POST("forgotpassword")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ForgotPasswordResponse>

    @POST("resetpassword")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<ResetPasswordResponse>

    @GET("view-image")
    suspend fun getImageLink(
        @Query("user_id") userId: String
    ): Response<ImageLinkResponse>

    @POST("get_prfl_dtls")
    suspend fun getProfileDetails(@Query("user_id") userId: Int): Response<GetProfileResponse>

    @POST("edt_prfl_dtls")
    suspend fun editProfileDetails(
        @Query("user_id") userId: String,
        @Body profileRequest: EditProfileRequest
    ): Response<EditProfileResponse>

    @POST("ChangeUserPassword")
    suspend fun changeUserPassword(@Body request: ChangePasswordRequest): Response<ChangePasswordResponse>

    @POST("location_details/")
    suspend fun getLocationDetails(@Body request: Map<String, Int>): Response<LocationResponse>


}

