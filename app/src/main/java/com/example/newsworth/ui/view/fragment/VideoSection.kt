package com.example.newsworth.ui.view.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.ImageModel
import com.example.newsworth.repository.NewsWorthCreatorRepository
import com.example.newsworth.ui.adapter.VideosItemAdapter
import com.example.newsworth.ui.viewmodel.NewsWorthCreatorViewModel
import com.example.newsworth.ui.viewmodel.NewsWorthCreatorViewModelFactory
import com.example.newsworth.ui.viewmodel.SharedViewModel
import com.example.newsworth.utils.SharedPrefModule

class VideoSection : Fragment(R.layout.fragment_video_section) {

    private lateinit var viewModel: NewsWorthCreatorViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels() // Use shared ViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        val recyclerView = view.findViewById<RecyclerView>(R.id.full_videos_recycler_view)

//          Set up RecyclerView with GridLayoutManager
//        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
//        recyclerView.layoutManager = gridLayoutManager
        val linearLayoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = linearLayoutManager

        // Observe the images data from the SharedViewModel
        sharedViewModel.imagesList.observe(viewLifecycleOwner) { items ->
            if (items.isNotEmpty()) {
                // Filter for items with Video_link
                val images = items.filter { !it.Video_link.isNullOrBlank() }
                recyclerView.adapter = VideosItemAdapter(images) // Set adapter with images data

            } else {
                Toast.makeText(requireContext(), "No videos found", Toast.LENGTH_SHORT).show()
            }
        }
        // Initialize ViewModel with ApiService from RetrofitClient
        val apiService = RetrofitClient.getApiService(requireContext())
        val repository = NewsWorthCreatorRepository(apiService)
        viewModel = ViewModelProvider(this, NewsWorthCreatorViewModelFactory(repository))[NewsWorthCreatorViewModel::class.java]
        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
        viewModel.fetchUploadedContent(userId)
        viewModel.uploadedContent.observe(viewLifecycleOwner) { response ->
            response.let {
                if (response != null) {
                    Toast.makeText(requireContext(), response.response, Toast.LENGTH_SHORT).show()
                    // Set the image data in SharedViewModel after fetching content
                    val imagesList = response.response_message.map { imageResponse ->
                        ImageModel(
                            content_title = imageResponse.content_title,
                            age_in_days = imageResponse.age_in_days,
                            gps_location = imageResponse.gps_location,
                            uploaded_by = imageResponse.uploaded_by,
                            content_description = imageResponse.content_description,
                            price = imageResponse.price,
                            discount = imageResponse.discount,
                            Video_link = imageResponse.Video_link)
                    }
                    sharedViewModel.setImagesList(imagesList) // Share the data with the ImagesFragment
                } else {
                    Toast.makeText(requireContext(), "Content upload failed", Toast.LENGTH_SHORT).show()
                    Log.e("UploadError", "API response: $response")
                }
            }
        }
        // Back Button Navigation
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}
