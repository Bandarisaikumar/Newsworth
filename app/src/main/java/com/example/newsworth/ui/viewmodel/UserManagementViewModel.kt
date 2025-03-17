package com.example.newsworth.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsworth.data.model.ForgotPasswordRequest
import com.example.newsworth.data.model.ForgotPasswordResponse
import com.example.newsworth.data.model.LoginRequest
import com.example.newsworth.data.model.LoginResponse
import com.example.newsworth.data.model.LogoutResponse
import com.example.newsworth.data.model.RegistrationModels
import com.example.newsworth.data.model.ResetPasswordRequest
import com.example.newsworth.data.model.ResetPasswordResponse
import com.example.newsworth.data.model.TokenResponse
import com.example.newsworth.repository.UserManagementRepository
import com.example.newsworth.utils.SharedPrefModule
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import retrofit2.Response
import android.content.Context
import com.example.newsworth.data.model.ErrorResponse


class UserManagementViewModel(private val repository: UserManagementRepository) : ViewModel() {

    val loginResponse = MutableLiveData<LoginResponse?>()
    val tokenResponse = MutableLiveData<TokenResponse?>()
    val userId = MutableLiveData<Int?>()


    private val _registrationResponse = MutableLiveData<RegistrationModels.RegistrationResponse>()
    val registrationResponse: LiveData<RegistrationModels.RegistrationResponse> =
        _registrationResponse

    private val _otpResponse = MutableLiveData<RegistrationModels.SendOtpResponse>()
    val otpResponse: LiveData<RegistrationModels.SendOtpResponse> = _otpResponse

    private val _verificationResponse =
        MutableLiveData<RegistrationModels.SignupVerificationResponse>()
    val verificationResponse: LiveData<RegistrationModels.SignupVerificationResponse> =
        _verificationResponse


    private val _logoutResponse = MutableLiveData<LogoutResponse>()
    val logoutResponse: LiveData<LogoutResponse> get() = _logoutResponse

    private val _forgotPasswordResponse = MutableLiveData<ForgotPasswordResponse?>()
    val forgotPasswordResponse: MutableLiveData<ForgotPasswordResponse?> = _forgotPasswordResponse


    private val _resetPasswordResponse = MutableLiveData<Response<ResetPasswordResponse>>()
    val resetPasswordResponse: LiveData<Response<ResetPasswordResponse>> = _resetPasswordResponse

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: MutableLiveData<String?> get() = _errorMessage


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _tokenError = MutableLiveData<ErrorResponse>()
    val tokenError: LiveData<ErrorResponse> = _tokenError

    private val _userId = MutableLiveData<Int?>()
    val userID: LiveData<Int?> = _userId

    fun registerUser(request: RegistrationModels.RegistrationRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.registerUser(request)
                _registrationResponse.value = response.body()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun sendOtp(request: RegistrationModels.SendOtpRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.sendOtp(request)
                _otpResponse.value = response.body()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifySignup(request: RegistrationModels.SignupVerificationRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.verifySignup(request)
                _verificationResponse.value = response.body()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(loginRequest: LoginRequest) {
        viewModelScope.launch {
            val response = repository.login(loginRequest)
            loginResponse.postValue(response)
            val userIdValue = response?.data?.firstOrNull()?.user_id
            userId.postValue(userIdValue)
        }

    }

    fun getAccessToken(tokenRequest: RequestBody) {
        viewModelScope.launch {
            val response = repository.getAccessToken(tokenRequest)
            tokenResponse.postValue(response)
        }
    }

    fun logoutUser(userId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.logoutUser(userId)
                if (response.isSuccessful && response.body()?.response == "success") {
                    _logoutResponse.postValue(response.body())
                } else {
                    _errorMessage.postValue("Logout failed: ${response.message()}")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error: ${e.message}")
            }
        }
    }

    fun forgotPassword(request: ForgotPasswordRequest, context: Context) {
        viewModelScope.launch {
            val response = repository.forgotPassword(request)
            _forgotPasswordResponse.postValue(response)
            val userIdValue = response?.data?.firstOrNull()?.userId
            Log.d("ForgotPassword", "Extracted user_id: $userIdValue")
            SharedPrefModule.provideTokenManager(context).userId2 = userIdValue.toString()
            userId.postValue(userIdValue)
        }
    }


    fun resetPassword(request: ResetPasswordRequest) {
        viewModelScope.launch {
            val response = repository.resetPassword(request)
            _resetPasswordResponse.postValue(response)
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
