package com.example.newsworth.ui.view.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
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
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
    private lateinit var searchView: SearchView
    private var originalItems: List<ImageModel> = emptyList()

    private var _binding: FragmentHomeContentBinding? = null
    private val binding get() = _binding!!
    private val uploadedContentLiveData =
        MutableLiveData<UploadedContentResponse?>()


    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val AUDIO_PERMISSION_REQUEST_CODE = 1002
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_VIDEO_CAPTURE = 2
    private var mediaRecorder: MediaRecorder? = null
    private var audioFileName: String? = null
    private lateinit var circleAdapter: CircleAdapter


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentHomeContentBinding.inflate(inflater, container, false)
        val view = binding.root

        searchView = view.findViewById(R.id.searchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val searchText = searchView.query.toString()
                if (searchText.isEmpty() || searchText.trim().isEmpty()) {
                    forceHideKeyboard(searchView.context, searchView)
                } else {
                    filterContent(searchText.trim())
                    forceHideKeyboard(searchView.context, searchView)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { searchQuery ->
                    filterContent(searchQuery.trim())
                }
                return true
            }
        })

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    Log.d("BackPressed", "Back button pressed")

                    if (::searchView.isInitialized) {
                        Log.d("BackPressed", "searchView.isIconified: ${searchView.isIconified}")

                        if (!searchView.isIconified) {
                            Log.d("BackPressed", "Collapsing search view")
                            searchView.setQuery("", false)
                            searchView.isIconified = true
                            searchView.clearFocus()
                        }

                        Log.d("BackPressed", "Showing exit dialog")
                        try {
                            AlertDialog.Builder(requireContext()).apply {
                                setTitle("Exit App")
                                setMessage("Are you sure you want to exit?")
                                setPositiveButton(android.R.string.yes) { dialog, which ->
                                    Log.d("BackPressed", "Exiting app")
                                    requireActivity().finishAffinity()
                                }
                                setNegativeButton(android.R.string.no, null)
                                setIcon(R.drawable.newsworthlogo)
                                create().show()
                            }
                        } catch (e: Exception) {
                            Log.e("BackPressed", "Error showing dialog: ${e.message}")
                        }
                    } else {
                        Log.e("BackPressed", "searchView is not initialized")
                    }
                }
            }
        )
        slideBar = view.findViewById(R.id.slideBar)
        iconLayout = view.findViewById(R.id.iconLayout)
        collapse = view.findViewById(R.id.collapse)
        main = view.findViewById(R.id.main)
        recyclerView = view.findViewById(R.id.recyclerView)
        progressBar = view.findViewById(R.id.progressBar)


        imagesAdapter = ImagesAdapter(emptyList())
        videosAdapter = VideosAdapter(emptyList())
        audiosAdapter = AudioAdapter(emptyList())

        binding.imagesRecyclerView.adapter = imagesAdapter
        binding.videosRecyclerView.adapter = videosAdapter
        binding.audiosRecyclerView.adapter = audiosAdapter

        binding.imagesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.videosRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.audiosRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)


        recyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val circleList = mutableListOf(
            CircleItem("Breaking News", R.drawable.breakingnews),
            CircleItem("Politics", R.drawable.politics),
            CircleItem("Business & Economy", R.drawable.business),
            CircleItem("Technology", R.drawable.technology),
            CircleItem("Science", R.drawable.science),
            CircleItem("Health", R.drawable.health),
            CircleItem("Education", R.drawable.education),
            CircleItem("Sports", R.drawable.sports),
            CircleItem("Entertainment", R.drawable.entertainment),
            CircleItem("Crime & Law", R.drawable.crime),
            CircleItem("World News", R.drawable.world),
            CircleItem("Environment", R.drawable.environment),
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
            ) { launchVideoCapture() }
        }
        view.findViewById<ImageView>(R.id.audioIcon).setOnClickListener {
            handlePermission(
                android.Manifest.permission.RECORD_AUDIO,
                AUDIO_PERMISSION_REQUEST_CODE
            ) { showAudioRecordingDialog() }
        }

        collapse.setOnClickListener {
            iconLayout.visibility = View.GONE
        }

        settingsButton = view.findViewById(R.id.settingsButton)
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
                viewModel.clearErrorMessage()
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                progressBar.bringToFront()
            }
        }


        return view
    }
    fun forceHideKeyboard(context: Context, view: android.view.View) {
        Handler(Looper.getMainLooper()).postDelayed({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (imm.isAcceptingText) {
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
            view.clearFocus()
        }, 100)
    }

    private fun selectDefaultCircleItem() {
        val defaultPosition = getCircleData().indexOfFirst { it.name == "Breaking News" }

        if (defaultPosition != -1) {
            circleAdapter.setSelectedPosition(defaultPosition)
            handleCircleItemClick(
                defaultPosition,
                getCircleData()[defaultPosition].name ?: "Unknown"
            )
        }
    }

    private fun handleCircleItemClick(position: Int, categoryName: String) {
        circleAdapter.setSelectedPosition(position)
        Log.d("CircleClick", "Circle item clicked at position: $position, Category: $categoryName")

        binding.category.text = categoryName

        searchView.setQuery("", false)
        searchView.isIconified = true
        searchView.clearFocus()

        displayContent(originalItems, categoryName)
    }

    private fun filterContent(query: String) {
        val trimmedQuery = query.trim()

        if (trimmedQuery.isEmpty()) {
            displayContent(originalItems, binding.category.text.toString())
            return
        }

        if (originalItems.isEmpty()) {
            return
        }

        val selectedCategory = binding.category.text.toString()
        val filteredList = originalItems.filter { item ->
            (item.content_title?.contains(trimmedQuery, ignoreCase = true) == true ||
                    item.content_description?.contains(trimmedQuery, ignoreCase = true) == true) &&
                    (selectedCategory == "All" || item.content_categories?.contains(selectedCategory) == true)
        }
        displayContent(filteredList, selectedCategory)
    }
    private fun fetchAndDisplayContent() {
        if (!isInternetAvailable()) {
            showNoInternetDialog()
            return
        }

        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toIntOrNull() ?: -1
        uploadedContentLiveData.removeObservers(viewLifecycleOwner)

        progressBar.visibility = View.VISIBLE
        progressBar.bringToFront()

        viewModel.fetchUploadedContent(userId)

        viewModel.uploadedContent.observe(viewLifecycleOwner) { response ->
            try {
                if (response != null && response.response == "success") {
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
                            content_categories = imageResponse.content_categories
                        )
                    }
                    originalItems = imagesList
                    val selectedCategory = binding.category.text.toString()
                    displayContent(imagesList, selectedCategory)
                } else {
                    Log.e("API_ERROR", "Response is null or not success")
                    Toast.makeText(requireContext(), "No content found", Toast.LENGTH_SHORT).show()
                    displayContent(emptyList(), "All")
                }
            } catch (e: Exception) {
                Log.e("ObserverError", "Error in observer: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                displayContent(emptyList(), "All")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayContent(items: List<ImageModel>, category: String) {
        Log.d("DISPLAY_CONTENT", "Items count: ${items.size}, Category: $category")

//        videosAdapter.releaseMediaPlayer()
        audiosAdapter.releaseMediaPlayer()

        val filteredItems = if (category == "All") {
            items
        } else {
            items.filter { item ->
                val categories = item.content_categories?.split(", ") ?: emptyList()
                categories.any { it.trim() == category }
            }
        }

        if (filteredItems.isEmpty()) {
            binding.noImagesText.visibility = View.VISIBLE
            binding.noVideosText.visibility = View.VISIBLE
            binding.noAudiosText.visibility = View.VISIBLE
            binding.imagesRecyclerView.visibility = View.GONE
            binding.videosRecyclerView.visibility = View.GONE
            binding.audiosRecyclerView.visibility = View.GONE
            return
        }

        val shuffledItems = filteredItems.shuffled()

        val images = shuffledItems.filter { !it.Image_link.isNullOrBlank() }.take(10)
        if (images.isEmpty()) {
            binding.noImagesText.visibility = View.VISIBLE
            binding.imagesRecyclerView.visibility = View.GONE
        } else {
            binding.noImagesText.visibility = View.GONE
            binding.imagesRecyclerView.visibility = View.VISIBLE
            imagesAdapter.updateImages(images)
        }

        val videos = shuffledItems.filter { !it.Video_link.isNullOrBlank() }.take(10)
        if (videos.isEmpty()) {
            binding.noVideosText.visibility = View.VISIBLE
            binding.videosRecyclerView.visibility = View.GONE
        } else {
            binding.noVideosText.visibility = View.GONE
            binding.videosRecyclerView.visibility = View.VISIBLE
            videosAdapter.updateVideos(videos)
        }

        val audios = shuffledItems.filter { !it.Audio_link.isNullOrBlank() }.take(10)
        if (audios.isEmpty()) {
            binding.noAudiosText.visibility = View.VISIBLE
            binding.audiosRecyclerView.visibility = View.GONE
        } else {
            binding.noAudiosText.visibility = View.GONE
            binding.audiosRecyclerView.visibility = View.VISIBLE
            audiosAdapter.updateAudios(audios)
        }
    }
    private fun getCircleData(): List<CircleItem> {
        return listOf(
            CircleItem("Breaking News", R.drawable.breakingnews),
            CircleItem("Politics", R.drawable.politics),
            CircleItem("Business & Economy", R.drawable.business),
            CircleItem("Technology", R.drawable.technology),
            CircleItem("Science", R.drawable.science),
            CircleItem("Health", R.drawable.health),
            CircleItem("Education", R.drawable.education),
            CircleItem("Sports", R.drawable.sports),
            CircleItem("Entertainment", R.drawable.entertainment),
            CircleItem("Crime & Law", R.drawable.crime),
            CircleItem("World News", R.drawable.world),
            CircleItem("Environment", R.drawable.environment),
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
            .setPositiveButton("OK") { _, _ -> }
        val alert = builder.create()
        alert.show()
    }

    fun showPositionSelectionDialog() {
        positionSelectionDialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_position_selection)
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        val topLeftImage = positionSelectionDialog.findViewById<ImageView>(R.id.topLeftImage)
        val topRightImage = positionSelectionDialog.findViewById<ImageView>(R.id.topRightImage)
        val bottomLeftImage = positionSelectionDialog.findViewById<ImageView>(R.id.bottomLeftImage)
        val bottomRightImage =
            positionSelectionDialog.findViewById<ImageView>(R.id.bottomRightImage)

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
        snapSlideBarToNearestCorner()
        determineCornerAndShowIconLayout()
        positionSelectionDialog.dismiss()
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
                slideBar.setImageResource(R.drawable.slidebarleft)
            }

            !isLeft && isTop -> {
                // Top-right corner
                slideBar.x = screenWidth - slideBar.width.toFloat()
                slideBar.y = 0f
                slideBar.setImageResource(R.drawable.rightsidee)
            }

            isLeft && !isTop -> {
                // Bottom-left corner
                slideBar.x = 0f
                slideBar.y = screenHeight - slideBar.height.toFloat()
                slideBar.setImageResource(R.drawable.slidebarleft)
            }

            else -> {
                // Bottom-right corner
                slideBar.x = screenWidth - slideBar.width.toFloat()
                slideBar.y = screenHeight - slideBar.height.toFloat()
                slideBar.setImageResource(R.drawable.rightsidee)
            }
        }
    }

    private fun determineCornerAndShowIconLayout() {
        iconLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val iconLayoutWidth = iconLayout.measuredWidth
        val iconLayoutHeight = iconLayout.measuredHeight

        val params =
            iconLayout.layoutParams as? RelativeLayout.LayoutParams ?: RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )

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

    private var tempVideoFile: File? = null

    private fun launchVideoCapture() {
        tempVideoFile = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "video_${System.currentTimeMillis()}.mp4"
        )
        val videoUri: Uri = FileProvider.getUriForFile(
            requireContext(),
            "com.gdrbnarmtech.newsworth.fileprovider",
            tempVideoFile!!
        )

        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)

        if (takeVideoIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
        } else {
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

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
        val bytes = file.readBytes()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
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
                    sharedPrefs.edit().putString("videoPathKey", videoPath).apply()
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


    override fun onDestroy() {
        super.onDestroy()
        if (::audiosAdapter.isInitialized) {
            audiosAdapter.releaseMediaPlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::audiosAdapter.isInitialized) {
            audiosAdapter.releaseMediaPlayer()
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
                    if (tempVideoFile != null && tempVideoFile!!.exists()) {
                        navigateToMediaUpload("Video", tempVideoFile!!.absolutePath)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to save video file",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode != Activity.RESULT_OK) {
            Toast.makeText(
                requireContext(),
                "Video capture cancelled or failed.",
                Toast.LENGTH_SHORT
            ).show()
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