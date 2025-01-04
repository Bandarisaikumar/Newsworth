package com.example.newsworth.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsworth.data.model.ChangePasswordRequest
import com.example.newsworth.data.model.ChangePasswordResponse
import com.example.newsworth.data.model.EditProfileRequest
import com.example.newsworth.data.model.EditProfileResponse
import com.example.newsworth.data.model.GetProfileResponse
import com.example.newsworth.data.model.ImageUploadResponse
import com.example.newsworth.data.model.LocationDetail
import com.example.newsworth.data.model.ProfileDetails
import com.example.newsworth.repository.ProfileManagementRepository
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import retrofit2.Response

class ProfileManagementViewmodel(private val profileRepository: ProfileManagementRepository) : ViewModel() {

    private val _uploadResponse = MutableLiveData<ImageUploadResponse>()
    val uploadResponse: LiveData<ImageUploadResponse> get() = _uploadResponse

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _imageUrl = MutableLiveData<String>()
    val imageUrl: LiveData<String> get() = _imageUrl

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _profileDetails = MutableLiveData<GetProfileResponse>()
    val profileDetails: LiveData<GetProfileResponse> get() = _profileDetails

    private val _editProfileResult = MutableLiveData<EditProfileResponse>()
    val editProfileResult: LiveData<EditProfileResponse> get() = _editProfileResult

    private val _editProfileData = MutableLiveData<ProfileDetails>() // Use the ProfileDetails type
    val editProfileData: LiveData<ProfileDetails> get() = _editProfileData

    val changePasswordResponse = MutableLiveData<ChangePasswordResponse>()
    val errorMessages = MutableLiveData<String>()

    private val _locationDetails = MutableLiveData<List<LocationDetail>>()
    val locationDetails: LiveData<List<LocationDetail>> get() = _locationDetails



    fun uploadProfileImage(userId: Int, file: MultipartBody.Part) {
        viewModelScope.launch {
            try {
                val response = profileRepository.uploadProfileImage(userId, file)
                if (response.isSuccessful) {
                    _uploadResponse.postValue(response.body())
                } else {
                    _errorMessage.postValue("Failed to upload profile image: ${response.message()}")
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.localizedMessage ?: "An unexpected error occurred")
            }
        }
    }
    fun fetchImageLink(userId: String) {
        viewModelScope.launch {
            val response = profileRepository.getImageLink(userId)
            if (response != null && response.response == "success") {
                _imageUrl.postValue(response.url) // Use the "url" field from the response
            } else {
                _error.postValue(response?.message ?: "Failed to fetch image link")
            }
        }
    }
    fun fetchProfileDetails(userId: Int) {
        viewModelScope.launch {
            val response = profileRepository.getProfileDetails(userId)
            if (response.isSuccessful) {
                response.body()?.let {
                    _profileDetails.postValue(it)
                }
            }
        }
    }

    fun updateProfileDetails(userId: String,profileRequest: EditProfileRequest) {
        viewModelScope.launch {
            val response = profileRepository.editProfileDetails(userId, profileRequest)
            if (response.isSuccessful) {
                response.body()?.let {
                    _editProfileResult.postValue(it)
                }
            }
        }
    }
    fun setEditProfileData(profileDetails: ProfileDetails) {
        _editProfileData.value = profileDetails
    }
    fun changePassword(request: ChangePasswordRequest) {
        viewModelScope.launch {
            try {
                val response = profileRepository.changeUserPassword(request)
                if (response.isSuccessful) {
                    changePasswordResponse.postValue(response.body())
                } else {
                    errorMessages.postValue("Change password failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                errorMessages.postValue("Error: ${e.message}")
            }
        }
    }

    fun fetchLocationDetails(pincode: Int) {
        viewModelScope.launch {
            val result = profileRepository.getLocationDetails(pincode)
            if (result != null && result.response == "success") {
                _locationDetails.postValue(result.data)
            } else {
                _errorMessage.postValue(result?.response_message ?: "Error fetching data")
            }
        }
    }

}