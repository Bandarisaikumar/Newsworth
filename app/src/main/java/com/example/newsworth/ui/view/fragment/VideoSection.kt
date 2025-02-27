package com.example.newsworth.ui.view.fragment

import android.app.AlertDialog
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.getSystemService
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var isInternetAvailable: Boolean = true
    private var isViewCreated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeScreen = parentFragment as? HomeScreen
                homeScreen?.showUserScreenForVideos()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isViewCreated = true

        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        val recyclerView = view.findViewById<RecyclerView>(R.id.full_videos_recycler_view)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        val linearLayoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = linearLayoutManager

        checkInternetAndSetup()

        sharedViewModel.imagesList.observe(viewLifecycleOwner) { items ->
            if (items.isNotEmpty()) {
                val videos = items.filter { !it.Video_link.isNullOrBlank() }
                recyclerView.adapter = VideosItemAdapter(videos)
            } else {
                Toast.makeText(requireContext(), "No videos found", Toast.LENGTH_SHORT).show()
                recyclerView.adapter = VideosItemAdapter(emptyList()) // Set empty adapter
            }
            swipeRefreshLayout.isRefreshing = false
        }

        swipeRefreshLayout.setOnRefreshListener {
            if (isInternetAvailable) {
                fetchData()
            } else {
                swipeRefreshLayout.isRefreshing = false
                showNoInternetDialog()
            }
        }

        backButton.setOnClickListener {
            val homeScreen = parentFragment as? HomeScreen
            homeScreen?.showUserScreenForVideos()
        }
    }

    private fun checkInternetAndSetup() {
        isInternetAvailable = isInternetAvailable()
        if (isInternetAvailable) {
            fetchData()
            enableSwipeRefresh()
        } else {
            showNoInternetDialog()
            disableSwipeRefresh()
        }
    }

    private fun enableSwipeRefresh() {
        if (isViewCreated) {
            swipeRefreshLayout.isEnabled = true
        } else {
            view?.post { swipeRefreshLayout.isEnabled = true }
        }
    }

    private fun disableSwipeRefresh() {
        if (isViewCreated) {
            swipeRefreshLayout.isEnabled = false
        } else {
            view?.post { swipeRefreshLayout.isEnabled = false }
        }
    }


    private fun fetchData() {
        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1

        if (!::viewModel.isInitialized) {
            val apiService = RetrofitClient.getApiService(requireContext())
            val repository = NewsWorthCreatorRepository(apiService)
            viewModel = ViewModelProvider(this, NewsWorthCreatorViewModelFactory(repository))[NewsWorthCreatorViewModel::class.java]
        }

        viewModel.fetchUploadedContent(userId)
        viewModel.uploadedContent.observe(viewLifecycleOwner) { response ->
            response.let {
                if (it != null) {
                    val imagesList = it.response_message.map { imageResponse ->
                        ImageModel(
                            content_title = imageResponse.content_title,
                            age_in_days = imageResponse.age_in_days,
                            gps_location = imageResponse.gps_location,
                            uploaded_by = imageResponse.uploaded_by,
                            content_description = imageResponse.content_description,
                            price = imageResponse.price,
                            discount = imageResponse.discount,
                            Video_link = imageResponse.Video_link
                        )
                    }
                    sharedViewModel.setImagesList(imagesList)
                } else {
                    Toast.makeText(requireContext(), "Content upload failed", Toast.LENGTH_SHORT).show()
                    Log.e("UploadError", "API response: $response")
                }
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showNoInternetDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("No Internet Connection")
            .setMessage("Please turn on your internet connection to continue.")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> }
        val alert = builder.create()
        alert.show()
    }

    override fun onResume() {
        super.onResume()
        checkInternetAndSetup()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isViewCreated = false
    }
}