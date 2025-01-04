package com.example.newsworth.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.newsworth.data.model.VideoItem

class VideosSectionViewModel : ViewModel() {

    private val _videoList = MutableLiveData<List<VideoItem>>()
    val videoList: LiveData<List<VideoItem>> get() = _videoList

    init {
        loadVideos()
    }

    private fun loadVideos() {
        _videoList.value = listOf(
            VideoItem("India to Play Historic Men's Test", "At Lord’s in 2025", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRiWNGJMGKn--HI9YJ83R7UjL6juQbYFOxw_Q&s"),
            VideoItem("India to Play Women's Test", "At Lord’s in 2025", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRiWNGJMGKn--HI9YJ83R7UjL6juQbYFOxw_Q&s"),
            VideoItem("Democrats criticize Trump", "Trial case goes viral", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRiWNGJMGKn--HI9YJ83R7UjL6juQbYFOxw_Q&s"),
            VideoItem("Former Airbase Evacuated", "Residents relocate", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRiWNGJMGKn--HI9YJ83R7UjL6juQbYFOxw_Q&s"),
            VideoItem("Earthquakes or Nuclear Tests?", "Speculations rise", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRiWNGJMGKn--HI9YJ83R7UjL6juQbYFOxw_Q&s"),
            VideoItem("India to Play Historic Men's Test", "At Lord’s in 2025", "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRiWNGJMGKn--HI9YJ83R7UjL6juQbYFOxw_Q&s")
        )
    }
}
