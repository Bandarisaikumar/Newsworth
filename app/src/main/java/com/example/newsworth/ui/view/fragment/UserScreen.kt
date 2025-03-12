package com.example.newsworth.ui.view.fragment

import android.app.AlertDialog
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.ImageModel
import com.example.newsworth.databinding.FragmentUserScreenBinding
import com.example.newsworth.repository.NewsWorthCreatorRepository
import com.example.newsworth.ui.adapter.AudioAdapter
import com.example.newsworth.ui.adapter.ImagesAdapter
import com.example.newsworth.ui.adapter.VideosAdapter
import com.example.newsworth.ui.viewmodel.NewsWorthCreatorViewModel
import com.example.newsworth.ui.viewmodel.NewsWorthCreatorViewModelFactory
import com.example.newsworth.ui.viewmodel.SharedViewModel
import com.example.newsworth.utils.SharedPrefModule
import kotlinx.coroutines.launch
import java.io.EOFException
import java.io.IOException
import java.net.SocketException
import javax.net.ssl.SSLHandshakeException

class UserScreen : Fragment() {

    private lateinit var binding: FragmentUserScreenBinding
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var viewModel: NewsWorthCreatorViewModel
    private lateinit var adapter: AudioAdapter
    private var isInternetAvailable: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeScreen = parentFragment as? HomeScreen
                homeScreen?.showHomeContentTab()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentUserScreenBinding.inflate(inflater, container, false)

        val apiService = RetrofitClient.getApiService(requireContext())
        val repository = NewsWorthCreatorRepository(apiService)
        viewModel = ViewModelProvider(
            this,
            NewsWorthCreatorViewModelFactory(repository)
        )[NewsWorthCreatorViewModel::class.java]

        checkInternetAndSetup()
        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            binding.swipeRefreshLayout.isEnabled = scrollY == 0
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            if (!isInternetAvailable()) {
                binding.swipeRefreshLayout.isRefreshing = false
                showNoInternetDialog()
            } else {
                initializeViewModelAndFetchData()
            }
        }
        setupRecyclerViews()
        setupButtonListeners()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (findNavController().currentDestination?.id == R.id.welcomeScreen) {
                findNavController().navigateUp()
            } else {
                findNavController().navigateUp()
            }
        }
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        return binding.root
    }

    private fun checkInternetAndSetup() {
        isInternetAvailable = isInternetAvailable()
        if (isInternetAvailable) {
            initializeViewModelAndFetchData()
        } else {
            showNoInternetDialog()
        }
    }

    private fun initializeViewModelAndFetchData() {
        if (!isInternetAvailable()) {
            binding.swipeRefreshLayout.isRefreshing = false
            showNoInternetDialog()
            return
        }
        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toIntOrNull() ?: -1
        viewModel.fetchUploadedContent(userId)

        viewModel.uploadedContent.observe(viewLifecycleOwner) { response ->
            viewLifecycleOwner.lifecycleScope.launch {
                binding.swipeRefreshLayout.isRefreshing = false
                try {
                    if (response != null) {
                        val imagesList = response.response_message.map { imageResponse ->
                            ImageModel(
                                content_title = imageResponse.content_title,
                                content_description = imageResponse.content_description,
                                age_in_days = imageResponse.age_in_days,
                                gps_location = imageResponse.gps_location,
                                uploaded_by = imageResponse.uploaded_by,
                                price = imageResponse.price,
                                discount = imageResponse.discount,
                                Image_link = imageResponse.Image_link,
                                Audio_link = imageResponse.Audio_link,
                                Video_link = imageResponse.Video_link,
                            )
                        }
                        sharedViewModel.setImagesList(imagesList)
                    } else {
                        Toast.makeText(requireContext(), "Content upload failed.", Toast.LENGTH_SHORT)
                            .show()
                        Log.e("UploadError", "API response is null")
                    }
                } catch (e: Exception) {
                    handleNetworkError(e)
                } finally {
                    viewModel._isLoading.value = false
                }
            }
        }
    }

    private fun handleNetworkError(e: Exception) {
        val errorMessage = when (e) {
            is IOException -> {
                when (e) {
                    is SSLHandshakeException -> "Secure connection failed. Please try again later."
                    is SocketException -> "Unable to connect to the server. Please check your internet connection."
                    is EOFException -> "The connection was closed unexpectedly. Please try again later."
                    else -> "A network error occurred. Please check your internet connection."
                }
            }
            else -> "An unexpected error occurred: ${e.message}"
        }
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        Log.e("FetchContent", "An error occurred: ${e.message}")
    }

    private fun setupRecyclerViews() = with(binding) {
        listOf(imagesRecyclerView, videosRecyclerView, audiosRecyclerView).forEach {
            it.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        sharedViewModel.imagesList.observe(viewLifecycleOwner) { items ->
            viewLifecycleOwner.lifecycleScope.launch {
                if (view != null) {
                    if (items.isNotEmpty()) {
                        val images = items.filter { !it.Image_link.isNullOrBlank() }.take(10)
                        imagesRecyclerView.adapter = ImagesAdapter(images)

                        val videos = items.filter { !it.Video_link.isNullOrBlank() }.take(10)
                        videosRecyclerView.adapter = VideosAdapter(videos)

                        val audios = items.filter { !it.Audio_link.isNullOrBlank() }.take(10)
                        audiosRecyclerView.adapter = AudioAdapter(audios)
                        adapter = AudioAdapter(audios)
                        audiosRecyclerView.adapter = adapter

                    } else {
                        Toast.makeText(requireContext(), "No content found", Toast.LENGTH_SHORT).show()
                        imagesRecyclerView.adapter = ImagesAdapter(emptyList())
                        videosRecyclerView.adapter = VideosAdapter(emptyList())
                        audiosRecyclerView.adapter = AudioAdapter(emptyList())
                    }
                }
            }
        }
    }

    private fun setupButtonListeners() = with(binding) {
        imagesMoreText.setOnClickListener {
            val homeScreen = parentFragment as? HomeScreen
            homeScreen?.navigateToImagesFragment()
        }
        videosMoreText.setOnClickListener {
            val homeScreen = parentFragment as? HomeScreen
            homeScreen?.navigateToVideosFragment()
        }
        audiosMoreText.setOnClickListener {
            val homeScreen = parentFragment as? HomeScreen
            homeScreen?.navigateToAudiosFragment()
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
            .setPositiveButton("OK") { _, _ ->
                // requireActivity().finish()
            }
        val alert = builder.create()
        alert.show()
    }

    override fun onStop() {
        super.onStop()
        if (::adapter.isInitialized) {
            adapter.releaseMediaPlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isInternetAvailable()) {
            showNoInternetDialog()
            binding.swipeRefreshLayout.isEnabled = false
        } else {
            binding.swipeRefreshLayout.isEnabled = true
            initializeViewModelAndFetchData()

        }

        if (::adapter.isInitialized) {
            adapter.releaseMediaPlayer()
        }
    }
    override fun onPause() {
        super.onPause()
        if (::adapter.isInitialized) {
            adapter.releaseMediaPlayer()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::adapter.isInitialized) {
            adapter.releaseMediaPlayer()
        }
    }

}