package com.example.newsworth.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.newsworth.data.model.VideoModel

class VideosViewModel : ViewModel() {

    // MutableLiveData to hold the list of videos
    private val _videosList = MutableLiveData<List<VideoModel>>()
    val videosList: LiveData<List<VideoModel>> get() = _videosList

    init {
        // Initialize with raw sample data
        loadVideos()
    }
    private fun loadVideos() {
        val sampleVideos = listOf(
            VideoModel("1", "Video 1", "https://sample-videos.com/video321/flv/720/big_buck_bunny_720p_1mb.flv"),
            VideoModel("2", "Video 2", "https://sample-videos.com/video321/flv/720/big_buck_bunny_720p_1mb.flv"),
            VideoModel("3", "Video 3", "https://sample-videos.com/video321/flv/720/big_buck_bunny_720p_1mb.flv"),
            VideoModel("4", "Video 4", "https://sample-videos.com/video321/flv/720/big_buck_bunny_720p_1mb.flv")
        )
        _videosList.value = sampleVideos
    }
}
