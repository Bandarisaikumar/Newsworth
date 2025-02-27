package com.example.newsworth.ui.view.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.CircleItem
import com.example.newsworth.data.model.ImageModel
import com.example.newsworth.data.model.UploadedContentResponse
import com.example.newsworth.databinding.FragmentHomeContentBinding
import com.example.newsworth.repository.NewsWorthCreatorRepository
import com.example.newsworth.ui.adapter.AudioAdapter
import com.example.newsworth.ui.adapter.CircleAdapter
import com.example.newsworth.ui.adapter.ImagesAdapter
import com.example.newsworth.ui.adapter.VideosAdapter
import com.example.newsworth.ui.viewmodel.NewsWorthCreatorViewModel
import com.example.newsworth.ui.viewmodel.NewsWorthCreatorViewModelFactory
import com.example.newsworth.utils.SharedPrefModule
import java.io.File
import java.io.FileInputStream

class HomeContent : Fragment() {

    private lateinit var slideBar: ImageView
    private lateinit var iconLayout: LinearLayout
    private lateinit var collapse: LinearLayout
    private lateinit var main: RelativeLayout
    private lateinit var settingsButton: ImageView
    private lateinit var positionSelectionDialog: Dialog
    private lateinit var viewModel: NewsWorthCreatorViewModel
    private lateinit var imagesAdapter: ImagesAdapter
    private lateinit var videosAdapter: VideosAdapter
    private lateinit var audiosAdapter: AudioAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private var _binding: FragmentHomeContentBinding? = null // Important: Make binding nullable
    private val binding get() = _binding!! // Use the non-null assertion operator
    private val uploadedContentLiveData =
        MutableLiveData<UploadedContentResponse?>() // Use MutableLiveData


    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val AUDIO_PERMISSION_REQUEST_CODE = 1002
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_VIDEO_CAPTURE = 2
    private var mediaRecorder: MediaRecorder? = null
    private var audioFileName: String? = null
    private lateinit var circleAdapter: CircleAdapter // Declare circleAdapter at the class level



    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentHomeContentBinding.inflate(inflater, container, false) // Initialize binding
        val view = binding.root // Use binding.root

        slideBar = view.findViewById(R.id.slideBar)
        iconLayout = view.findViewById(R.id.iconLayout)
        collapse = view.findViewById(R.id.collapse) // Reference to the collapse LinearLayout
        main = view.findViewById(R.id.main)
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)



        // Initialize Adapters *BEFORE* fetching data
        imagesAdapter = ImagesAdapter(emptyList())
        videosAdapter = VideosAdapter(emptyList())
        audiosAdapter = AudioAdapter(emptyList())

        binding.imagesRecyclerView.adapter = imagesAdapter
        binding.videosRecyclerView.adapter = videosAdapter
        binding.audiosRecyclerView.adapter = audiosAdapter

        // *** Add these lines ***
        binding.imagesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.videosRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.audiosRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)


        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val circleList = mutableListOf(
            CircleItem("General News", R.drawable.general),
            CircleItem("Entertainment", R.drawable.entertainment),
            CircleItem("Sports", R.drawable.sports),
            CircleItem("Business", R.drawable.business),
            CircleItem("Health", R.drawable.health),
            CircleItem("Educational", R.drawable.education),
            CircleItem("Fashion", R.drawable.fastion),
            CircleItem("Location", R.drawable.location)
        )

        circleAdapter = CircleAdapter(circleList) { position ->
            handleCircleItemClick(position, circleList[position].name ?: "Unknown")
        }
        recyclerView.adapter = circleAdapter




        // Set the initial position of the slideBar to the bottom-left corner
        slideBar.post {
            screenWidth = view.width
            screenHeight = view.height

            // Set the initial position of the slideBar
            slideBar.x = 0f
            slideBar.y = screenHeight - slideBar.height.toFloat()

            // Ensure the iconLayout is positioned correctly initially
            determineCornerAndShowIconLayout()
            iconLayout.visibility = View.GONE // Hide it initially
        }

        slideBar.setOnTouchListener(object : View.OnTouchListener {
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var offsetX: Float = 0f
            private var offsetY: Float = 0f
            private val CLICK_THRESHOLD = 10f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        offsetX = slideBar.x - slideBar.left
                        offsetY = slideBar.y - slideBar.top
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val newX = event.rawX - initialTouchX + offsetX
                        val newY = event.rawY - initialTouchY + offsetY

                        val maxX = screenWidth - slideBar.width
                        val maxY = screenHeight - slideBar.height
                        slideBar.x = newX.coerceIn(0f, maxX.toFloat())
                        slideBar.y = newY.coerceIn(0f, maxY.toFloat())

                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val deltaX = Math.abs(event.rawX - initialTouchX)
                        val deltaY = Math.abs(event.rawY - initialTouchY)

                        // Snap the slideBar to the nearest corner
                        snapSlideBarToNearestCorner()

                        // If the movement was within the click threshold, treat it as a click
                        if (deltaX < CLICK_THRESHOLD && deltaY < CLICK_THRESHOLD) {
                            // Show the iconLayout beside the slideBar
                            determineCornerAndShowIconLayout()
                        } else {
                            // Hide the iconLayout if the slideBar was dragged (not clicked)
                            iconLayout.visibility = View.GONE
                        }

                        return true
                    }
                }
                return true
            }
        })

        // Set click listeners for the icons inside iconLayout
        view.findViewById<ImageView>(R.id.imageIcon).setOnClickListener {
            handlePermission(
                android.Manifest.permission.CAMERA,
                CAMERA_PERMISSION_REQUEST_CODE
            ) { launchCameraForImage() }
        }
        view.findViewById<ImageView>(R.id.videoIcon).setOnClickListener {
            handlePermission(
                android.Manifest.permission.CAMERA,
                CAMERA_PERMISSION_REQUEST_CODE
            ) { launchCameraForVideo() }
        }
        view.findViewById<ImageView>(R.id.audioIcon).setOnClickListener {
            handlePermission(
                android.Manifest.permission.RECORD_AUDIO,
                AUDIO_PERMISSION_REQUEST_CODE
            ) { showAudioRecordingDialog() }
        }

        // Set click listener for the collapse LinearLayout
        collapse.setOnClickListener {
            // Hide the iconLayout when the collapse LinearLayout is clicked
            iconLayout.visibility = View.GONE
        }
        settingsButton = view.findViewById(R.id.settingsButton) // Initialize settings button
        settingsButton.setOnClickListener {
            showPositionSelectionDialog()
        }

        val apiService = RetrofitClient.getApiService(requireContext())
        val repository = NewsWorthCreatorRepository(apiService)
        viewModel = ViewModelProvider(
            this,
            NewsWorthCreatorViewModelFactory(repository)
        )[NewsWorthCreatorViewModel::class.java]
        
        fetchAndDisplayContent()
        selectDefaultCircleItem()

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage() // Clear the error after displaying it
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                progressBar.bringToFront() // Add this line to bring the progress bar to the front
            }
        }


        return view
    }
    private fun selectDefaultCircleItem() {
        // Find the position of "General News" in the circleList
        val defaultPosition = getCircleData().indexOfFirst { it.name == "General News" }

        if (defaultPosition != -1) {
            circleAdapter.setSelectedPosition(defaultPosition)
            handleCircleItemClick(defaultPosition, getCircleData()[defaultPosition].name ?: "Unknown") // Trigger content fetch
        }
    }
    private fun handleCircleItemClick(position: Int, categoryName: String) {
        circleAdapter.setSelectedPosition(position)
        Log.d("CircleClick", "Circle item clicked at position: $position, Category: $categoryName")

        binding.category.text = categoryName
        fetchAndDisplayContent()
    }
    private fun fetchAndDisplayContent() {
        if (!isInternetAvailable()) {
            showNoInternetDialog()
            return
        }


        val userId =
            SharedPrefModule.provideTokenManager(requireContext()).userId?.toIntOrNull() ?: -1
        uploadedContentLiveData.removeObservers(viewLifecycleOwner)

        progressBar.visibility = View.VISIBLE // Show the progress bar here!
        progressBar.bringToFront()

        viewModel.fetchUploadedContent(userId) // Fetch ALL content

        viewModel.uploadedContent.observe(viewLifecycleOwner) { response ->
            try {
                Log.d("API_RESPONSE", response.toString()) // Log the entire response

                if (response != null) { // Check for null response FIRST
                    response.let { // Now it's safe to use let
                        if (it.response == "success") {
                            val imagesList = it.response_message.map { imageResponse ->
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
                                    Video_link = imageResponse.Video_link
                                )
                            }
                            displayContent(imagesList) // Display content
                        } else {
                            val errorMessage = when (it.response) { // Use it.response safely
                                null -> "Response message is null" // Handle null response message
                                else -> "API returned an error: ${it.response}" // Existing error handling
                            }

                            Log.e("API_ERROR", errorMessage)
                            Toast.makeText(requireContext(), "No content found", Toast.LENGTH_SHORT).show()
                            displayContent(emptyList()) // Clear RecyclerViews
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Response is null") // Log null response
                    Toast.makeText(requireContext(), "No content found", Toast.LENGTH_SHORT).show()
                    displayContent(emptyList()) // Clear RecyclerViews
                }
            } catch (e: Exception) {
                Log.e("ObserverError", "Error in observer: ${e.message}")
                Toast.makeText(requireContext(), "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
                displayContent(emptyList()) // Clear RecyclerViews
            }finally {
                progressBar.visibility = View.GONE // Hide the progress bar here!
            }
        }
    }

    private fun displayContent(items: List<ImageModel>) {
        Log.d("DISPLAY_CONTENT", "Items count: ${items.size}")

        // Release MediaPlayers if necessary (only for Video and Audio adapters)
        videosAdapter.releaseMediaPlayer()
        audiosAdapter.releaseMediaPlayer()

        val shuffledItems = items.shuffled()

        val images = shuffledItems.filter { !it.Image_link.isNullOrBlank() }.take(10)
        Log.d("DISPLAY_CONTENT", "Images count: ${images.size}")
        imagesAdapter.updateImages(images) // Update the existing adapter

        val videos = shuffledItems.filter { !it.Video_link.isNullOrBlank() }.take(10)
        Log.d("DISPLAY_CONTENT", "Videos count: ${videos.size}")
        videosAdapter.updateVideos(videos) // Update the existing adapter

        val audios = shuffledItems.filter { !it.Audio_link.isNullOrBlank() }.take(10)
        Log.d("DISPLAY_CONTENT", "Audios count: ${audios.size}")
        audiosAdapter.updateAudios(audios) // Update the existing adapter

        if (items.isEmpty()) {
            Toast.makeText(requireContext(), "No content found", Toast.LENGTH_SHORT).show()
        }
    }
    private fun getCircleData(): List<CircleItem> {
        return listOf(
            CircleItem("General News", R.drawable.general),
            CircleItem("Entertainment", R.drawable.entertainment),
            CircleItem("Sports", R.drawable.sports),
            CircleItem("Business", R.drawable.business),
            CircleItem("Health", R.drawable.health),
            CircleItem("Educational", R.drawable.education),
            CircleItem("Fashion", R.drawable.fastion),
            CircleItem("Location", R.drawable.location)
        )
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showNoInternetDialog() {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("No Internet Connection")
            .setMessage("Please turn on your internet connection to continue.")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> } // You might want to add an action here
        val alert = builder.create()
        alert.show()
    }

    private fun showPositionSelectionDialog() {
        positionSelectionDialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_position_selection) // Create a new layout for the dialog
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        // Initialize ImageViews for the four positions
        val topLeftImage = positionSelectionDialog.findViewById<ImageView>(R.id.topLeftImage)
        val topRightImage = positionSelectionDialog.findViewById<ImageView>(R.id.topRightImage)
        val bottomLeftImage = positionSelectionDialog.findViewById<ImageView>(R.id.bottomLeftImage)
        val bottomRightImage =
            positionSelectionDialog.findViewById<ImageView>(R.id.bottomRightImage)

        // Set click listeners for each position
        topLeftImage.setOnClickListener { setSlideBarPosition(0f, 0f) }
        topRightImage.setOnClickListener {
            setSlideBarPosition(
                screenWidth - slideBar.width.toFloat(),
                0f
            )
        }
        bottomLeftImage.setOnClickListener {
            setSlideBarPosition(
                0f,
                screenHeight - slideBar.height.toFloat()
            )
        }
        bottomRightImage.setOnClickListener {
            setSlideBarPosition(
                screenWidth - slideBar.width.toFloat(),
                screenHeight - slideBar.height.toFloat()
            )
        }

        positionSelectionDialog.show()
    }

    private fun setSlideBarPosition(x: Float, y: Float) {
        slideBar.x = x
        slideBar.y = y
        snapSlideBarToNearestCorner() // Optional: Snap to the corner after manual placement
        determineCornerAndShowIconLayout() // Crucial: Call this to update iconLayout position
        positionSelectionDialog.dismiss() // Close the dialog
    }


    private fun snapSlideBarToNearestCorner() {
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        // Determine the nearest corner based on the slideBar's current position
        val isLeft = slideBar.x < centerX
        val isTop = slideBar.y < centerY

        // Snap the slideBar to the nearest corner
        when {
            isLeft && isTop -> {
                // Top-left corner
                slideBar.x = 0f
                slideBar.y = 0f
                slideBar.setImageResource(R.drawable.slidebarleft) // Set the correct image
            }

            !isLeft && isTop -> {
                // Top-right corner
                slideBar.x = screenWidth - slideBar.width.toFloat()
                slideBar.y = 0f
                slideBar.setImageResource(R.drawable.rightsidee) // Set the correct image
            }

            isLeft && !isTop -> {
                // Bottom-left corner
                slideBar.x = 0f
                slideBar.y = screenHeight - slideBar.height.toFloat()
                slideBar.setImageResource(R.drawable.slidebarleft) // Set the correct image
            }

            else -> {
                // Bottom-right corner
                slideBar.x = screenWidth - slideBar.width.toFloat()
                slideBar.y = screenHeight - slideBar.height.toFloat()
                slideBar.setImageResource(R.drawable.rightsidee) // Set the correct image
            }
        }
    }

    private fun determineCornerAndShowIconLayout() {
        // Ensure the iconLayout's dimensions are calculated
        iconLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val iconLayoutWidth = iconLayout.measuredWidth
        val iconLayoutHeight = iconLayout.measuredHeight

        val params =
            iconLayout.layoutParams as? RelativeLayout.LayoutParams ?: RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )

        // Determine the position of the iconLayout based on the slideBar's corner
        when {
            slideBar.x == 0f && slideBar.y == 0f -> {
                // Top-left corner: Place iconLayout to the right of the slideBar
                params.leftMargin = slideBar.width
                params.topMargin = 0
            }

            slideBar.x == screenWidth - slideBar.width.toFloat() && slideBar.y == 0f -> {
                // Top-right corner: Place iconLayout to the left of the slideBar
                params.leftMargin = screenWidth - slideBar.width - iconLayoutWidth
                params.topMargin = 0
            }

            slideBar.x == 0f && slideBar.y == screenHeight - slideBar.height.toFloat() -> {
                // Bottom-left corner: Place iconLayout to the right of the slideBar
                params.leftMargin = slideBar.width
                params.topMargin = screenHeight - iconLayoutHeight
            }

            else -> {
                // Bottom-right corner: Place iconLayout to the left of the slideBar
                params.leftMargin = screenWidth - slideBar.width - iconLayoutWidth
                params.topMargin = screenHeight - iconLayoutHeight
            }
        }

        iconLayout.layoutParams = params
        iconLayout.visibility = View.VISIBLE
    }

    private fun handlePermission(permission: String, requestCode: Int, action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(permission), requestCode)
        } else action()
    }

    private fun launchCameraForImage() =
        startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_IMAGE_CAPTURE)

    private fun launchCameraForVideo() =
        startActivityForResult(Intent(MediaStore.ACTION_VIDEO_CAPTURE), REQUEST_VIDEO_CAPTURE)

    private fun showAudioRecordingDialog() {
        createDialog(R.layout.dialog_audio_recording) { dialog ->
            val button = dialog.findViewById<TextView>(R.id.btn_start_stop_recording)
            var isRecording = false
            button.text = "Start Recording"

            button.setOnClickListener {
                try {
                    if (isRecording) stopAudioRecording(dialog) else startAudioRecording()
                    isRecording = !isRecording
                    button.text = if (isRecording) "Stop Recording" else "Start Recording"
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("AudioRecording", "Error: ${e.message}")
                }
            }
        }
    }

    private fun createDialog(layout: Int, setup: (Dialog) -> Unit) {
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(layout)
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setOnDismissListener { main.visibility = View.VISIBLE }
        }
        main.visibility = View.INVISIBLE
        setup(dialog)
        dialog.show()
    }

    private fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_REQUEST_CODE
            )
            return
        }

        val audioFile = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            "audio_recording_${System.currentTimeMillis()}.aac"
        )
        audioFileName = audioFile.absolutePath
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFileName)
            prepare()
            start()
        }
        Toast.makeText(requireContext(), "Recording started", Toast.LENGTH_SHORT).show()
    }

    private fun stopAudioRecording(dialog: Dialog) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecording", "Error stopping recorder: ${e.message}")
        } finally {
            mediaRecorder = null
        }

        dialog.dismiss()

        val aacFile = File(audioFileName)
        if (aacFile.exists()) {
            val mp3File = File(
                requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "audio_recording_${System.currentTimeMillis()}.mp3"
            )
            val command = "-i ${aacFile.absolutePath} -acodec libmp3lame ${mp3File.absolutePath}"
            val result = FFmpeg.execute(command)

            if (result == 0 && mp3File.exists()) {
                // Convert to Base64 and pass to next screen
                val base64Audio = convertToBase64(mp3File)
                passAudioToNextScreen(base64Audio)
            } else {
                Toast.makeText(context, "Failed to convert audio to MP3", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun passAudioToNextScreen(base64Audio: String) {
        val bundle = Bundle().apply {
            putString("audioBase64", base64Audio)
            putString("mediaType", "Audio")
        }
        val homeScreen = parentFragment as? HomeScreen
        homeScreen?.loadMediaUploadFragment(bundle)
    }

    private fun convertToBase64(file: File): String {
        val bytes = file.readBytes() // Read file bytes
        return Base64.encodeToString(bytes, Base64.DEFAULT) // Convert to Base64 string
    }

    private fun navigateToMediaUpload(mediaType: String, media: Any) {
        val sharedPrefs = requireContext().getSharedPreferences("MediaPrefs", Context.MODE_PRIVATE)
        val bundle = Bundle().apply {
            putString("mediaType", mediaType)
            when (mediaType) {
                "Image" -> {
                    val imagePath = saveBitmapToFile(media as Bitmap).absolutePath
                    val base64Image = encodeFileToBase64(imagePath)
                    putString("base64Data", base64Image)
                }

                "Video" -> {
                    val videoPath = media as String
                    // Save the video path to SharedPreferences
                    sharedPrefs.edit().putString("videoPathKey", videoPath).apply()
                    // Pass the key in the Bundle
                    putString("pathKey", "videoPathKey")
                }
            }
        }
        val homeScreen = parentFragment as? HomeScreen
        homeScreen?.loadMediaUploadFragment(bundle)
    }

    private fun encodeFileToBase64(filePath: String): String {
        val file = File(filePath)
        val bytes = FileInputStream(file).use { it.readBytes() }
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val file = File(requireContext().cacheDir, "temp_image.png")
        Log.d("FilePath", "Saving image to: ${file.absolutePath}")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        if (file.exists()) {
            Log.d("FilePath", "File saved successfully: ${file.absolutePath}")
        } else {
            Log.e("FilePath", "Failed to save the file.")
        }
        return file
    }

    // Function to save video to a temporary file and get its absolute path
    private fun saveVideoToFile(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File(
                requireContext().cacheDir,
                "video_${System.currentTimeMillis()}.mp4" // Unique file name with timestamp
            )
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile.absolutePath // Return the absolute path of the new file
        } catch (e: Exception) {
            Log.e("VideoSaveError", "Failed to save video: ${e.message}")
            null
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    (data?.extras?.get("data") as? Bitmap)?.let {
                        navigateToMediaUpload(
                            "Image",
                            it
                        )
                    }
                }

                REQUEST_VIDEO_CAPTURE -> {
                    data?.data?.let { videoUri ->
                        val videoFilePath = saveVideoToFile(videoUri) // Save new video
                        if (videoFilePath != null) {
                            Log.d(
                                "VideoFilePath",
                                "Saved video file path: $videoFilePath"
                            ) // Log the file path
                            navigateToMediaUpload("Video", videoFilePath) // Use the new file path
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Failed to save video file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun navigate(actionId: Int) = findNavController().navigate(actionId)

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                CAMERA_PERMISSION_REQUEST_CODE -> launchCameraForImage()
                AUDIO_PERMISSION_REQUEST_CODE -> showAudioRecordingDialog()
            }
        }
    }

}