package com.example.newsworth.ui.view.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.MultiAutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.MetadataRequest
import com.example.newsworth.databinding.FragmentMediaUploadScreenBinding
import com.example.newsworth.repository.NewsWorthCreatorRepository
import com.example.newsworth.ui.viewmodel.NewsWorthCreatorViewModel
import com.example.newsworth.ui.viewmodel.NewsWorthCreatorViewModelFactory
import com.example.newsworth.ui.viewmodel.SharedViewModel
import com.example.newsworth.utils.SharedPrefModule
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaUploadFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: FragmentMediaUploadScreenBinding
    private lateinit var viewModel: NewsWorthCreatorViewModel
    private val LOCATION_PERMISSION_REQUEST_CODE = 1003
    private var contentId: Int = 0
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var sharedViewModel: SharedViewModel // SharedViewModel
    // Add these variables for categories
    private lateinit var autoCompleteTextView: MultiAutoCompleteTextView
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private val categoryList = mutableListOf<String>()
    private val selectedCategoryIds = mutableListOf<Int>() // Store selected category IDs


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeScreen = parentFragment as? HomeScreen
                homeScreen?.showHomeContentTab()
            }
        })
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMediaUploadScreenBinding.inflate(inflater, container, false)
        binding.root.setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Initialize ViewModel with ApiService from RetrofitClient
        val apiService = RetrofitClient.getApiService(requireContext())
        val repository = NewsWorthCreatorRepository(apiService)
        viewModel = ViewModelProvider(
            this,
            NewsWorthCreatorViewModelFactory(repository)
        )[NewsWorthCreatorViewModel::class.java]

        // Initialize SharedViewModel to share data between fragments
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        autoCompleteTextView = binding.autoCompleteCategories
        autoCompleteTextView.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList)
        autoCompleteTextView.setAdapter(categoryAdapter)

        autoCompleteTextView.keyListener = null
        autoCompleteTextView.setOnClickListener { autoCompleteTextView.showDropDown() }

        fetchCategories()

        // Set up item click listener for AutoCompleteTextView
        autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categoryAdapter.getItem(position)
            selectedCategory?.let {
                val categoryId = getCategoryIdByName(it)
                if (categoryId != -1) {
                    selectedCategoryIds.add(categoryId)
                }
            }
        }

        val mediaType = arguments?.getString("mediaType")

        when (mediaType) {
            "Image" -> {
                val base64Data = arguments?.getString("base64Data")
                base64Data?.let {
                    displayImage(base64Data)
                } ?: Toast.makeText(requireContext(), "Image data not found", Toast.LENGTH_SHORT)
                    .show()
            }


            "Video" -> {
                // Retrieve the video path key from the arguments
                val pathKey = arguments?.getString("pathKey")
                if (pathKey == "videoPathKey") {
                    // Get the video path from SharedPreferences
                    val sharedPrefs =
                        requireContext().getSharedPreferences("MediaPrefs", Context.MODE_PRIVATE)
                    val videoPath = sharedPrefs.getString(pathKey, null)

                    videoPath?.let {
                        val videoFile = File(it) // Get the video file from the absolute path
                        if (videoFile.exists()) {
                            playVideo(Uri.fromFile(videoFile)) // Play the video using the file URI
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Video file not found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } ?: Toast.makeText(
                        requireContext(),
                        "Video data not found",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }


            "Audio" -> {
                val base64Audio = arguments?.getString("audioBase64")

                base64Audio?.let {
                    // Decode the Base64 string to an audio file
                    val audioFile = decodeBase64ToFile(it, "audio_recording.mp3")
                    if (audioFile.exists()) {
                        playAudio(Uri.fromFile(audioFile))
                    } else {
                        Toast.makeText(requireContext(), "Audio file not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                } ?: Toast.makeText(requireContext(), "Audio data not found", Toast.LENGTH_SHORT)
                    .show()
            }


            else -> Toast.makeText(requireContext(), "No media to display", Toast.LENGTH_SHORT)
                .show()
        }


        binding.backButton.setOnClickListener {
            val homeScreen = parentFragment as? HomeScreen
            homeScreen?.showHomeContentTab()
        }


        binding.btnUpload.setOnClickListener {
            // Stop audio playback if it's playing
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                    it.release()
                    mediaPlayer = null
                    Toast.makeText(requireContext(), "Audio playback stopped", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            if (!isInternetAvailable()) {  // Check internet *before* proceeding
                showNoInternetToast() // Or a more specific message
                return@setOnClickListener // Stop execution if no internet
            }

            // Validate that all fields are filled
            val title = binding.etTitle.text.toString()
            val description = binding.etDescription.text.toString()
            val priceText = binding.etPrice.text.toString()
            val discountText = binding.etDiscount.text.toString()
            val tags = binding.etTags.text.toString()

            val selectedCategories = autoCompleteTextView.text.toString().split(",").map { it.trim() }
            val selectedCategoryIds = selectedCategories.mapNotNull { categoryName ->
                viewModel.categories.value?.find { it.category_name == categoryName }?.category_id
            }


            if (title.isEmpty() || tags.isEmpty() || selectedCategoryIds.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all the required fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Get reference to the ProgressBar
            val progressBar = view?.findViewById<ProgressBar>(R.id.progressBar)
            progressBar?.visibility = View.VISIBLE

            // Validate price as a valid float
            val price = priceText.toFloatOrNull()
            if (price == null) {
                Toast.makeText(requireContext(), "Invalid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate discount as a valid integer
            val discount = if (discountText.isNotEmpty()) discountText.toIntOrNull() ?: 0 else 0

            val categoriesString = selectedCategoryIds.joinToString(",")


            // Trigger metadata and location calls
            observeMetadataResponse() // Triggers the metadata call and updates contentId
            getLocationDetails() // Triggers the metadata insertion

            // Observe contentId updates
            viewModel.metadataResult.observe(viewLifecycleOwner) { response ->
                response?.let {
                    contentId = it.content_id

                    val userId =
                        SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
                    val contentType = mediaType

                    // Ensure the file path is not null and use the Base64 encoded string or file path
                    val filePathOrBase64 = getFilePath() ?: run {
                        Toast.makeText(requireContext(), "No file selected", Toast.LENGTH_SHORT)
                            .show()
                        return@observe
                    }

                    val filebase64_file = filePathOrBase64
                    Log.d("File", filebase64_file.toString())

                    // Replace with your actual Base64 string
                    val base64String = filebase64_file // Example: Your Base64 string
                    val fileName = "audio_file.txt" // Example: File name to save the string

                    // Call the function to save the Base64 string
                    context?.let { it1 -> saveBase64ToFile(it1, base64String.toString(), fileName) }

//                    val contentRequest = ContentUploadRequestBody(file = filePathOrBase64.toString(), tags = tags)

                    // Show confirmation dialog before uploading (optional)
//                    showContentRequestDialog(contentRequest)
                    progressBar?.visibility = View.VISIBLE

                    // Trigger content upload using ViewModel
                    viewModel.uploadContent(
                        userId, contentId, title,
                        contentType.toString(), description, price, discount,
                        categoriesString,
                        filebase64_file.toString(), tags
                    )
                } ?: Toast.makeText(
                    requireContext(),
                    "Metadata insertion failed",
                    Toast.LENGTH_SHORT
                ).show()
            }


            // Observe the API response
            viewModel.contentUploadResponse.observe(viewLifecycleOwner) { response ->
                progressBar?.visibility = View.GONE

                response?.let {
                    Toast.makeText(requireContext(), response.response_message, Toast.LENGTH_SHORT)
                        .show()
                    try {
                        val homeScreenFragment = parentFragment as? HomeScreen
                        homeScreenFragment?.showMyFilesTab()
                    } catch (e: Exception) {
                        Log.e("NavigationError", "Error during navigation: ${e.localizedMessage}")
                        Toast.makeText(requireContext(), "Navigation failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                } ?: run {
                    Toast.makeText(requireContext(), "Content upload failed", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("UploadError", "API response: $response")
                }
            }

        }

        return binding.root
    }
    private fun showNoInternetToast() {
        Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
    }
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun decodeBase64ToFile(base64String: String, fileName: String): File {
        val audioBytes = Base64.decode(base64String, Base64.DEFAULT)
        val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)

        // Write the decoded bytes to the file
        file.outputStream().use { it.write(audioBytes) }

        return file
    }

    fun saveBase64ToFile(context: Context, base64String: String, fileName: String) {
        val file = File(context.getExternalFilesDir(null), fileName)
        file.writeText(base64String)
        println("File saved at: ${file.absolutePath}")
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun displayImage(base64Data: String) {
        try {
            // Decode the Base64 string back to a Bitmap
            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

            // Display the image in the ImageView
            binding.imageView.visibility = View.VISIBLE
            binding.videoView.visibility = View.GONE
            binding.btnPlayAudio.visibility = View.GONE
            binding.imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to decode image: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    private fun playVideo(videoUri: Uri) {
        try {
            binding.videoView.visibility = View.VISIBLE
            binding.imageView.visibility = View.GONE
            binding.btnPlayAudio.visibility = View.GONE

            binding.videoView.setVideoURI(videoUri)
            binding.videoView.start()

            binding.videoView.setOnPreparedListener {
                // The video is prepared, you can show any loading indicators if necessary
                Toast.makeText(requireContext(), "Video is ready to play", Toast.LENGTH_SHORT)
                    .show()
            }

            binding.videoView.setOnErrorListener { mp, what, extra ->
                Toast.makeText(requireContext(), "Error playing video", Toast.LENGTH_SHORT).show()
                false
            }

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to play video: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun playAudio(audioUri: Uri) {
        try {
            binding.videoView.visibility = View.GONE
            binding.imageView.visibility = View.GONE
            binding.btnPlayAudio.visibility = View.VISIBLE

            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.apply {
                    setDataSource(requireContext(), audioUri)
                    prepare()
                }
            }

            binding.btnPlayAudio.setOnClickListener {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                        binding.btnPlayAudio.setImageResource(R.drawable.play_button)
                        Toast.makeText(requireContext(), "Audio Paused", Toast.LENGTH_SHORT).show()
                    } else {
                        it.start()
                        binding.btnPlayAudio.setImageResource(R.drawable.pause_button)
                        Toast.makeText(requireContext(), "Audio Playing", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            mediaPlayer?.setOnCompletionListener {
                Toast.makeText(requireContext(), "Audio Finished", Toast.LENGTH_SHORT).show()
                binding.btnPlayAudio.setImageResource(R.drawable.play_button)
                mediaPlayer?.reset()
                mediaPlayer?.setDataSource(requireContext(), audioUri)
                mediaPlayer?.prepare()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to play audio: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getLocationDetails() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latitude = it.latitude
                    val longitude = it.longitude
                    val altitude = it.altitude
                    val uploadedTime = getCurrentTime()
                    val incidentTime = getCurrentTime()
                    val userId =
                        SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1

                    val metadata = createMetadataRequest(
                        userId,
                        latitude,
                        longitude,
                        altitude,
                        uploadedTime,
                        incidentTime
                    )
                    viewModel.insertMetadata(metadata)  // Trigger ViewModel to upload metadata
                } ?: Toast.makeText(requireContext(), "Location not available", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun createMetadataRequest(
        userId: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        uploadedTime: String,
        incidentTime: String
    ): MetadataRequest {
        val gpsLocation = getAddressFromCoordinates(latitude, longitude) ?: "Unknown Location"
        val mobileType = getMobileType()
        val mobileOS = "Android ${Build.VERSION.RELEASE}"
        val brand = Build.BRAND
        val resolution =
            "${resources.displayMetrics.widthPixels} x ${resources.displayMetrics.heightPixels}"

        return MetadataRequest(
            user_id = userId,
            gps_location = gpsLocation,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            mobile_type = mobileType,
            mobile_os = mobileOS,
            brand = brand,
            resolution = resolution,
            uploaded_time = uploadedTime,
            incident_time = incidentTime
        )
    }

    private fun showDetailsDialog(
        userId: Int,
        contentId: Int,
        title: String,
        contentType: String?,
        description: String,
        price: Float,
        discount: Int,
        filebase64_file: String,
        tags: String
    ) {
        val dialog = Dialog(requireContext())
        dialog.apply {
            setContentView(R.layout.dialog_device_details)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Format the details into a readable string
            val details = """
            userId : $userId
            contentId :$contentId    
            Title: $title
            contentType : $contentType
            Description: $description
            Price: $price
            Discount: $discount
            Filepath: $filebase64_file
            Tags: $tags
        """.trimIndent()

            findViewById<TextView>(R.id.detailsTextView).text = details
            findViewById<TextView>(R.id.okButton).setOnClickListener { dismiss() }
        }
        dialog.show()
    }


    private fun getAddressFromCoordinates(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val addressLines = (0..address.maxAddressLineIndex)
                    .map { address.getAddressLine(it) }
                    .joinToString(", ")

                // Remove Plus Codes (e.g., "CFPX+WXH")
                val regex = "^[A-Z0-9]{4}\\+\\w{2,3}".toRegex()
                if (regex.containsMatchIn(addressLines)) {
                    return addressLines.replaceFirst(regex, "").trimStart().replace(",", "", true)
                } else addressLines
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getMobileType(): String {
        val model = Build.MODEL // Device model
        val manufacturer = Build.MANUFACTURER // Device manufacturer
        return "$manufacturer $model" // Concatenate manufacturer and model
    }
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }

    private fun observeMetadataResponse() {
        viewModel.metadataResult.observe(viewLifecycleOwner) { response ->
            response?.let {
                contentId = it.content_id
//
//                Toast.makeText(requireContext(), "Metadata insertion success", Toast.LENGTH_SHORT)
//                    .show()
            } ?: run {
                Toast.makeText(requireContext(), "Metadata insertion failed", Toast.LENGTH_SHORT)
                    .show()
            }
            if (response != null) {
                Toast.makeText(requireContext(), response.response_message, Toast.LENGTH_SHORT)
                    .show()

            } else {
                Toast.makeText(requireContext(), "Metadata insertion failed", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun showResponseDialog(message: String, contentId: String) {
        val dialog = Dialog(requireContext())
        dialog.apply {
            setContentView(R.layout.dialog_response)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            findViewById<TextView>(R.id.responseTextView).text = message
            findViewById<TextView>(R.id.contentIdTextView).text = contentId
            findViewById<TextView>(R.id.okButton).setOnClickListener { dismiss() }
        }
        dialog.show()
    }

    // Helper function to get real file path from URI
    private fun getRealPathFromURI(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndex(MediaStore.Video.Media.DATA)
            if (columnIndex != -1) {
                it.moveToFirst()
                return it.getString(columnIndex)
            }
        }

        // If the URI is a content URI, use ContentResolver to get input stream and write to a temp file
        if ("content" == uri.scheme) {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val tempFile = File.createTempFile("video_", ".mp4", requireContext().cacheDir)
                val outputStream = FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                return tempFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

    private fun getFilePath(): String? {
        val mediaType = arguments?.getString("mediaType")
        val base64Data = arguments?.getString("base64Data")

        return when (mediaType) {
            "Image" -> {
                base64Data // Directly return the Base64 string for Image
            }

            "Video" -> {
                val pathKey = arguments?.getString("pathKey")
                val sharedPrefs =
                    requireContext().getSharedPreferences("MediaPrefs", Context.MODE_PRIVATE)

                if (pathKey == "videoPathKey") {
                    // Retrieve the video path from SharedPreferences
                    val videoPath = sharedPrefs.getString("videoPathKey", null)
                    videoPath?.let {
                        // Convert the file path string into a File object
                        val videoFile = File(it)

                        // Call compress and encode video
                        compressVideoAndEncode(videoFile)
                    }
                } else {
                    null
                }

            }

            "Audio" -> {
                val base64Audio = arguments?.getString("audioBase64")
                base64Audio ?: "" // Return the Base64 string for audio
            }

            else -> null // Return null for unsupported media types
        }
    }


    fun encodeFileToBase64(file: File): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024 * 8)  // 8KB buffer for chunking
        var inputStream: InputStream? = null

        try {
            inputStream = FileInputStream(file)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // Convert to Base64 after reading all the chunks
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
    }


    fun compressVideoAndEncode(file: File): String {
        // Define the output file path for the compressed video
        val outputFile = File(
            requireContext().cacheDir,
            "compressed_video_${System.currentTimeMillis()}.mp4"
        ) // Unique file name

        // FFmpeg command for video compression
        val command = arrayOf(
            "-i", file.absolutePath,       // Input file
            "-vcodec", "libx264",           // Video codec
            "-crf", "28",                   // Quality setting (lower means higher quality)
            "-preset", "fast",              // Speed of compression
            "-s", "1280x720",               // Reduce resolution to 1280x720
            "-y",                           // Overwrite the output file without asking
            outputFile.absolutePath        // Output file
        )

        // Execute FFmpeg command and check if it was successful
        val result = FFmpeg.execute(command)
        if (result != 0) {
            Log.e("VideoCompression", "Video compression failed with result code: $result")
            return "" // Return empty string if compression failed
        }

        // Check if the compressed video file exists
        return if (outputFile.exists()) {
            encodeFileToBase64(outputFile)  // Convert the compressed video to Base64
        } else {
            Log.e("VideoCompression", "Compressed video file does not exist.")
            ""
        }
    }
    private fun fetchCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categories?.let {
                categoryList.clear()
                categoryList.addAll(it.map { category -> category.category_name })
                categoryAdapter.notifyDataSetChanged()
            } ?: run {
                Toast.makeText(requireContext(), "Failed to fetch categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCategoryIdByName(categoryName: String): Int {
        viewModel.categories.value?.let { categories ->
            categories.forEach { category ->
                if (category.category_name == categoryName) {
                    return category.category_id
                }
            }
        }
        return -1
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationDetails()
        }
    }
}
