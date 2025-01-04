package com.example.newsworth.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsworth.data.model.ContentUploadResponse
import com.example.newsworth.data.model.MetadataRequest
import com.example.newsworth.data.model.MetadataResponse
import com.example.newsworth.data.model.UploadedContentResponse
import com.example.newsworth.repository.NewsWorthCreatorRepository
import com.example.newsworth.utils.NetworkUtils.retryIO
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class NewsWorthCreatorViewModel(private val repository: NewsWorthCreatorRepository) : ViewModel() {

    private val _metadataResult = MutableLiveData<MetadataResponse?>()
    val metadataResult: LiveData<MetadataResponse?> = _metadataResult

    // LiveData to observe API response
    private val _contentUploadResponse = MutableLiveData<ContentUploadResponse>()
    val contentUploadResponse: LiveData<ContentUploadResponse> get() = _contentUploadResponse

    private val _uploadedContent = MutableLiveData<UploadedContentResponse>()
    val uploadedContent: LiveData<UploadedContentResponse> get() = _uploadedContent

    // LiveData for error handling
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error


    fun insertMetadata(metadata: MetadataRequest) {
        viewModelScope.launch {
            try {
                val response = repository.uploadMetadata(metadata)
                if (response.isSuccessful) {
                    _metadataResult.postValue(response.body())
                } else {
                    _metadataResult.postValue(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _metadataResult.postValue(null)
            }
        }
    }

    fun uploadContent(
        userId: Int,
        contentId: Int,
        title: String,
        mediaType: String?,
        description: String,
        price: Float,
        discount: Int,
        filebase64_file: String,
        tags: String
    ) {
        viewModelScope.launch {
            try {
                retryIO {

                    Log.d("UploadContent", "Starting uploadContent with title: $title")

                    // Create the requestBody for tags
                    val tagsRequestBody = tags.toRequestBody("text/plain".toMediaTypeOrNull())

                    // Prepare the file part for the upload
                    val base64ImageRequestBody =
                        filebase64_file.toRequestBody("text/plain".toMediaTypeOrNull())

                    // Upload the content
                    val response = mediaType?.let {
                        repository.uploadContent(
                            userId = userId,                 // Query param
                            contentId = contentId,           // Query param
                            title = title,                   // Query param
                            mediaType = it,                  // Query param (
                            description = description,       // Query param
                            price = price,                   // Query param
                            discount = discount,             // Query param
                            filebase64_file = base64ImageRequestBody,   // Form data
                            tags = tagsRequestBody           // Form data
                        )
                    }

                    // Handle the response
                    if (response != null) {
                        if (response.isSuccessful) {
                            Log.d("UploadContent", "Upload successful: ${response.body()}")
                            _contentUploadResponse.postValue(response.body())
                        } else {
                            Log.e("UploadContent", "Upload failed: ${response.message()}")
                            _error.postValue("Upload failed: ${response.message()}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UploadContent", "An error occurred: ${e.localizedMessage}")
                _error.postValue("An error occurred: ${e.localizedMessage}")
            }
        }
    }
    fun fetchUploadedContent(userId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.fetchUploadedContent(userId)
                if (response.isSuccessful) {
                    _uploadedContent.postValue(response.body())
                } else {
                    Log.e("FetchContent", "Error fetching content: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("FetchContent", "An error occurred: ${e.localizedMessage}")
            }
        }
    }

}
