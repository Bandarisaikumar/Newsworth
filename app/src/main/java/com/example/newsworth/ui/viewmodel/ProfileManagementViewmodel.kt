package com.example.newsworth.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsworth.data.model.ChangePasswordRequest
import com.example.newsworth.data.model.ChangePasswordResponse
import com.example.newsworth.data.model.EditProfileRequest
import com.example.newsworth.data.model.EditProfileResponse
import com.example.newsworth.data.model.GetProfileResponse
import com.example.newsworth.data.model.ImageLinkResponse
import com.example.newsworth.data.model.ImageUploadResponse
import com.example.newsworth.data.model.LocationDetail
import com.example.newsworth.data.model.ProfileDetails
import com.example.newsworth.repository.ProfileManagementRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.io.IOException
import java.net.SocketException
import javax.net.ssl.SSLHandshakeException

class ProfileManagementViewmodel(private val profileRepository: ProfileManagementRepository) :
    ViewModel() {

    private val _uploadResponse = MutableLiveData<ImageUploadResponse>()
    val uploadResponse: LiveData<ImageUploadResponse> get() = _uploadResponse

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: MutableLiveData<String?> get() = _errorMessage

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _profileDetails = MutableLiveData<GetProfileResponse?>()
    val profileDetails: LiveData<GetProfileResponse?> = _profileDetails

    private val _imageUrl = MutableLiveData<ImageLinkResponse?>()
    val imageUrl: LiveData<ImageLinkResponse?> = _imageUrl

    private val _editProfileResult = MutableLiveData<EditProfileResponse>()
    val editProfileResult: LiveData<EditProfileResponse> get() = _editProfileResult

    private val _editProfileData = MutableLiveData<ProfileDetails>()
    val editProfileData: LiveData<ProfileDetails> get() = _editProfileData

    val changePasswordResponse = MutableLiveData<ChangePasswordResponse>()
    val errorMessages = MutableLiveData<String>()

    private val _locationDetails = MutableLiveData<List<LocationDetail>>()
    val locationDetails: LiveData<List<LocationDetail>> get() = _locationDetails

    val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading


    fun uploadProfileImage(userId: Int, file: MultipartBody.Part) {
        viewModelScope.launch {
            _isLoading.postValue(true) // Added loading start
            try {
                val response = profileRepository.uploadProfileImage(userId, file)
                if (response.isSuccessful) {
                    _uploadResponse.postValue(response.body())
                } else {
                    _errorMessage.postValue("Failed to upload profile image: ${response.message()}")
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.localizedMessage ?: "An unexpected error occurred")
            } finally {
                _isLoading.postValue(false) // Added loading end
            }
        }
    }

    fun fetchProfileDetails(userId: Int) {
        viewModelScope.launch {
            _isLoading.postValue(true) // Added loading start
            try {
                val response = profileRepository.getProfileDetails(userId)
                if (response.isSuccessful) {
                    _profileDetails.postValue(response.body())
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Bad Request"
                        else -> "Error fetching profile details: ${response.message()}"
                    }
                    Log.e("ProfileDetails", errorMessage)
                    _error.postValue(errorMessage)
                    _profileDetails.postValue(null)
                }
            } catch (e: Exception) {
                Log.e("ProfileDetails", "An error occurred: ${e.message}")
                val errorMessage = when (e) {
                    is IOException -> {
                        if (e is SSLHandshakeException) {
                            "There was a problem establishing a secure connection. Please try again later."
                        } else if (e is SocketException) {
                            "Unable to connect to the server. Please check your internet connection."
                        } else {
                            "A network error occurred. Please check your internet connection."
                        }
                    }
                    else -> "An unexpected error occurred: ${e.message}"
                }
                _error.postValue(errorMessage)
                _profileDetails.postValue(null)
            } finally {
                _isLoading.postValue(false) // Added loading end
            }
        }
    }

    fun fetchImageLink(userId: String) {
        viewModelScope.launch {
            _isLoading.postValue(true) // Added loading start
            try {
                val response = profileRepository.getImageLink(userId)
                if (response.isSuccessful) {
                    _imageUrl.postValue(response.body())
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Bad Request"
                        else -> "Error fetching image link: ${response.message()}"
                    }
                    Log.e("ImageLink", errorMessage)
                    _error.postValue(errorMessage)
                    _imageUrl.postValue(null)
                }
            } catch (e: Exception) {
                Log.e("ImageLink", "An error occurred: ${e.message}")
                val errorMessage = when (e) {
                    is IOException -> {
                        if (e is SSLHandshakeException) {
                            "There was a problem establishing a secure connection. Please try again later."
                        } else if (e is SocketException) {
                            "Unable to connect to the server. Please check your internet connection."
                        } else {
                            "A network error occurred. Please check your internet connection."
                        }
                    }
                    else -> "An unexpected error occurred: ${e.message}"
                }
                _error.postValue(errorMessage)
                _imageUrl.postValue(null)
            } finally {
                _isLoading.postValue(false) // Added loading end
            }
        }
    }

    fun updateProfileDetails(userId: String, profileRequest: EditProfileRequest) {
        viewModelScope.launch {
            _isLoading.postValue(true) // Added loading start
            try {
                val response = profileRepository.editProfileDetails(userId, profileRequest)
                if (response.isSuccessful) {
                    response.body()?.let {
                        _editProfileResult.postValue(it)
                    }
                }
            } finally {
                _isLoading.postValue(false) // Added loading end
            }
        }
    }

    fun setEditProfileData(profileDetails: ProfileDetails) {
        _editProfileData.value = profileDetails
    }

    fun changePassword(request: ChangePasswordRequest) {
        viewModelScope.launch {
            _isLoading.postValue(true) // Added loading start
            try {
                val response = profileRepository.changeUserPassword(request)
                if (response.isSuccessful) {
                    changePasswordResponse.postValue(response.body())
                } else {
                    errorMessages.postValue("Change password failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                errorMessages.postValue("Error: ${e.message}")
            } finally {
                _isLoading.postValue(false) // Added loading end
            }
        }
    }

    fun fetchLocationDetails(pincode: Int) {
        viewModelScope.launch {
            _isLoading.postValue(true) // Added loading start
            try {
                val result = profileRepository.getLocationDetails(pincode)
                if (result != null && result.response == "success") {
                    _locationDetails.postValue(result.data)
                } else {
                    _errorMessage.postValue(result?.response_message ?: "Error fetching data")
                }
            } finally {
                _isLoading.postValue(false) // Added loading end
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}