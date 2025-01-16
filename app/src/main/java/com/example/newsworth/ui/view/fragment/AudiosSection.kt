package com.example.newsworth.ui.view.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.ImageModel
import com.example.newsworth.repository.NewsWorthCreatorRepository
import com.example.newsworth.ui.adapter.AudioAdapter
import com.example.newsworth.ui.adapter.AudiosItemAdapter
import com.example.newsworth.ui.adapter.ImagesAdapter
import com.example.newsworth.ui.adapter.ImagesItemAdapter
import com.example.newsworth.ui.viewmodel.NewsWorthCreatorViewModel
import com.example.newsworth.ui.viewmodel.NewsWorthCreatorViewModelFactory
import com.example.newsworth.ui.viewmodel.SharedViewModel
import com.example.newsworth.utils.SharedPrefModule

class AudiosSection : Fragment(R.layout.fragment_audios_section) {


    private lateinit var viewModel: NewsWorthCreatorViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels() // Use shared ViewModel
    private lateinit var adapter: AudiosItemAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        val recyclerView = view.findViewById<RecyclerView>(R.id.full_audios_recycler_view)
        val swipeRefreshLayout = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)


//        // Set up RecyclerView with GridLayoutManager
//        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
//        recyclerView.layoutManager = gridLayoutManager
        val linearLayoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = linearLayoutManager

        // Observe data from ViewModel
        // Observe the images data from the SharedViewModel
        sharedViewModel.imagesList.observe(viewLifecycleOwner) { items ->
            if (items.isNotEmpty()) {
                // Filter for items with Image_link
                val images = items.filter { !it.Audio_link.isNullOrBlank() }
                recyclerView.adapter = AudiosItemAdapter(images) // Set adapter with images data
                adapter = AudiosItemAdapter(images)
                recyclerView.adapter = adapter

            } else {
                Toast.makeText(requireContext(), "No audios found", Toast.LENGTH_SHORT).show()
            }
            swipeRefreshLayout.isRefreshing = false // Stop refresh animation

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
                    Toast.makeText(requireContext(), response.response, Toast.LENGTH_SHORT)
                        .show()
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
                            Image_link = imageResponse.Image_link,
                            Audio_link = imageResponse.Audio_link)
                    }

                    sharedViewModel.setImagesList(imagesList) // Share the data with the ImagesFragment
                } else {
                    Toast.makeText(requireContext(), "Content upload failed", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("UploadError", "API response: $response")
                }
                swipeRefreshLayout.isRefreshing = false // Stop refresh animation

            }
        }
        // Back Button Navigation
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }
        // Set up SwipeRefreshLayout listener
        swipeRefreshLayout.setOnRefreshListener {
            // Refresh data when pulled
            viewModel.fetchUploadedContent(userId)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::adapter.isInitialized) {
            adapter.releaseMediaPlayer() // Release resources only if adapter is initialized
        }
    }

    override fun onStop() {
        super.onStop()
        if (::adapter.isInitialized) {
            adapter.releaseMediaPlayer() // Release resources only if adapter is initialized
        }
    }

}
