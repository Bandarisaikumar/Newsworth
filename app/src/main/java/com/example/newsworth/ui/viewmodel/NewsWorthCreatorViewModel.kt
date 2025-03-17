package com.example.newsworth.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsworth.data.model.Category
import com.example.newsworth.data.model.ContentUploadResponse
import com.example.newsworth.data.model.MetadataRequest
import com.example.newsworth.data.model.MetadataResponse
import com.example.newsworth.data.model.UploadedContentResponse
import com.example.newsworth.repository.NewsWorthCreatorRepository
import com.example.newsworth.utils.NetworkUtils.retryIO
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.net.ssl.SSLHandshakeException
import java.net.SocketException
import javax.inject.Inject

class NewsWorthCreatorViewModel @Inject constructor(private val repository: NewsWorthCreatorRepository) :
    ViewModel() {

    private val _metadataResult = MutableLiveData<MetadataResponse?>()
    val metadataResult: LiveData<MetadataResponse?> = _metadataResult

    private val _contentUploadResponse = MutableLiveData<ContentUploadResponse>()
    val contentUploadResponse: LiveData<ContentUploadResponse> = _contentUploadResponse

    private val _uploadedContent = MutableLiveData<UploadedContentResponse>()
    val uploadedContent: LiveData<UploadedContentResponse> = _uploadedContent

    private val _categories = MutableLiveData<List<Category>?>()
    val categories: MutableLiveData<List<Category>?> = _categories

    private val _error = MutableLiveData<String?>()
    val error: MutableLiveData<String?> = _error

    val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun insertMetadata(metadata: MetadataRequest) {
        viewModelScope.launch {
            try {
                val response = repository.uploadMetadata(metadata)
                if (response.isSuccessful) {
                    _metadataResult.postValue(response.body())
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Bad Request"
                        401 -> "Unauthorized"
                        403 -> "Forbidden"
                        500 -> "Internal Server Error"
                        else -> "Metadata upload failed: ${response.message()}"
                    }
                    Log.e("MetadataUpload", errorMessage)
                    _error.postValue(errorMessage)
                    _metadataResult.postValue(null)
                }
            } catch (e: Exception) {
                Log.e("MetadataUpload", "An error occurred: ${e.message}")
                _error.postValue("An error occurred: ${e.message}")
                _metadataResult.postValue(null)
            }
        }
    }

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                val categoryList = repository.getCategories()
                _categories.postValue(categoryList)
            } catch (e: Exception) {
                Log.e("CategoryFetchError", "Error fetching categories: ${e.message}")
                _categories.postValue(null)
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
        content_category_ids: String,
        filebase64_file: String,
        tags: String
    ) {
        viewModelScope.launch {
            try {
                retryIO {

                    Log.d("UploadContent", "Starting uploadContent with title: $title")

                    val tagsRequestBody = tags.toRequestBody("text/plain".toMediaTypeOrNull())
                    val base64ImageRequestBody =
                        filebase64_file.toRequestBody("text/plain".toMediaTypeOrNull())
                    val categoriesRequestBody = content_category_ids.toRequestBody("application/json".toMediaTypeOrNull())


                    val response = mediaType?.let {
                        repository.uploadContent(
                            userId,
                            contentId,
                            title,
                            it,
                            description,
                            price,
                            discount,
                            categoriesRequestBody,
                            base64ImageRequestBody,
                            tagsRequestBody
                        )
                    }

                    if (response != null) {
                        if (response.isSuccessful) {
                            Log.d("UploadContent", "Upload successful: ${response.body()}")
                            _contentUploadResponse.postValue(response.body())
                        } else {
                            val errorMessage = when (response.code()) {
                                400 -> "Bad Request"
                                401 -> "Unauthorized"
                                403 -> "Forbidden"
                                500 -> "Internal Server Error"
                                else -> "Upload failed: ${response.message()}"
                            }
                            Log.e("UploadContent", errorMessage)
                            _error.postValue(errorMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UploadContent", "An error occurred: ${e.message}")

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

                    else -> "An unexpected error occurred. Please try again later."
                }
                _error.postValue(errorMessage)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun fetchUploadedContent(userId: Int) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val response = repository.fetchUploadedContent(userId)
                if (response.isSuccessful) {
                    _uploadedContent.postValue(response.body())
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Bad Request"
                        401 -> "Unauthorized"
                        403 -> "Forbidden"
                        500 -> "Internal Server Error"
                        else -> "Error fetching content: ${response.message()}"
                    }
                    Log.e("FetchContent", errorMessage)
                    _error.postValue(errorMessage)
                }
            } catch (e: Exception) {
                Log.e("FetchContent", "An error occurred: ${e.message}")
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
                    else -> "You have not uploaded any content yet."
                }
                _error.postValue(errorMessage)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearErrorMessage() {
        _error.value = null
    }
}