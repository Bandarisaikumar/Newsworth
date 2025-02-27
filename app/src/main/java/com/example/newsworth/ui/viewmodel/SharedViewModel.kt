package com.example.newsworth.ui.viewmodel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.newsworth.data.model.ImageModel

class SharedViewModel : ViewModel() {

    // MutableLiveData to hold the list of images
    private val _imagesList = MutableLiveData<List<ImageModel>>()
    val imagesList: LiveData<List<ImageModel>> get() = _imagesList

    // Function to set the images list
    fun setImagesList(images: List<ImageModel>) {
        _imagesList.value = images
    }
    // Function to get only the first 10 images
    fun getFirstTenImages(): LiveData<List<ImageModel>> {
        return MutableLiveData(_imagesList.value?.take(10) ?: emptyList())
    }
}
