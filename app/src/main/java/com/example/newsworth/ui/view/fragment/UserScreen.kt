package com.example.newsworth.ui.view.fragment

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arthenica.mobileffmpeg.FFmpeg
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
import java.io.File
import java.io.FileInputStream

class UserScreen : Fragment() {

    private lateinit var binding: FragmentUserScreenBinding
    private val sharedViewModel: SharedViewModel by activityViewModels() // Use shared ViewModel
    private lateinit var viewModel: NewsWorthCreatorViewModel

    private lateinit var adapter: AudioAdapter

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_VIDEO_CAPTURE = 2
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val AUDIO_PERMISSION_REQUEST_CODE = 1002
    private var mediaRecorder: MediaRecorder? = null
    private var audioFileName: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentUserScreenBinding.inflate(inflater, container, false)

        arguments?.getBoolean("continueUpload")?.let { continueUpload ->
            if (continueUpload) showUploadPopup()
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
                            content_description = imageResponse.content_description,
                            age_in_days = imageResponse.age_in_days,
                            gps_location = imageResponse.gps_location,
                            uploaded_by = imageResponse.uploaded_by,
                            price = imageResponse.price,
                            discount = imageResponse.discount,
                            Image_link = imageResponse.Image_link,
                            Audio_link = imageResponse.Audio_link,
                            Video_link = imageResponse.Video_link)
                    }

                    sharedViewModel.setImagesList(imagesList) // Share the data with the ImagesFragment
                } else {
                    Toast.makeText(requireContext(), "Content upload failed", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("UploadError", "API response: $response")
                }
                binding.swipeRefreshLayout.isRefreshing = false // Stop the refresh animation

            }
        }
        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            binding.swipeRefreshLayout.isEnabled = scrollY == 0
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            if (binding.scrollView.scrollY == 0) {
                viewModel.fetchUploadedContent(userId)
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        setupRecyclerViews()
        setupButtonListeners()
        binding.homeImage.setColorFilter(ContextCompat.getColor(requireContext(), R.color.mehrun_color), android.graphics.PorterDuff.Mode.SRC_IN)


        // Handle the back gesture
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // If this is the first screen, navigate to the previous screen (SecondScreen)
            if (findNavController().currentDestination?.id == R.id.welcomeScreen) {
                findNavController().navigateUp() // Navigate back in the navigation stack

            } else {
                findNavController().navigateUp() // Navigate back in the navigation stack
            }
        }

        return binding.root
    }

    private fun setupRecyclerViews() = with(binding) {
        listOf(imagesRecyclerView, videosRecyclerView, audiosRecyclerView).forEach {
            it.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
//        imagesViewModel.imagesList.observe(viewLifecycleOwner) {
//            imagesRecyclerView.adapter = ImagesAdapter(it)
//        }
        // Observe the images data from the SharedViewModel
        sharedViewModel.imagesList.observe(viewLifecycleOwner) { items ->
            if (items.isNotEmpty()) {
                // Filter for items with Image_link
                val images = items.filter { !it.Image_link.isNullOrBlank() }.take(10)
                imagesRecyclerView.adapter = ImagesAdapter(images) // Set adapter with images data

            } else {
                Toast.makeText(requireContext(), "No Images found", Toast.LENGTH_SHORT).show()
            }
        }
        sharedViewModel.imagesList.observe(viewLifecycleOwner) { items ->
            if (items.isNotEmpty()) {
                // Filter for items with Image_link
                val videos = items.filter { !it.Video_link.isNullOrBlank() }.take(10)
                videosRecyclerView.adapter = VideosAdapter(videos) // Set adapter with images data

            } else {
                Toast.makeText(requireContext(), "No Videos found", Toast.LENGTH_SHORT).show()
            }
        }
//        audiosViewModel.audioList.observe(viewLifecycleOwner) {
//            audiosRecyclerView.adapter = AudioAdapter(it, requireContext())
//        }
//        sharedViewModel.imagesList.observe(viewLifecycleOwner) { items ->
//            if (items.isNotEmpty()) {
//                // Filter for items with Audio_link
//                val audios = items.filter { !it.Audio_link.isNullOrBlank() }.take(10)
//                audiosRecyclerView.adapter = AudioAdapter(audios) // Set adapter with audios data
//
//            } else {
//                Toast.makeText(requireContext(), "No audios found", Toast.LENGTH_SHORT).show()
//            }
//            // Handle audios
//            val audios = items.filter { !it.Audio_link.isNullOrBlank() }.take(10)
//            if (audios.isNotEmpty()) {
//                adapter = AudioAdapter(audios)
//                audiosRecyclerView.adapter = adapter // Set adapter for audios
//            } else {
//                Toast.makeText(requireContext(), "No audios found", Toast.LENGTH_SHORT).show()
//                adapter = AudioAdapter(emptyList()) // Set empty list if no audios
//                audiosRecyclerView.adapter = adapter // Set empty adapter
//            }
//        }
        sharedViewModel.imagesList.observe(viewLifecycleOwner) { items ->
            if (items.isNotEmpty()) {
                // Filter for items with Audio_link
                val audios = items.filter { !it.Audio_link.isNullOrBlank() }.take(10)
                if (::adapter.isInitialized) {
                    audiosRecyclerView.adapter = adapter // Set adapter if it's initialized
                } else {
                    adapter = AudioAdapter(audios)
                    audiosRecyclerView.adapter = adapter // Initialize the adapter if not initialized
                }
            } else {
                Toast.makeText(requireContext(), "No audios found", Toast.LENGTH_SHORT).show()
                adapter = AudioAdapter(emptyList()) // Set empty list if no audios
                audiosRecyclerView.adapter = adapter // Set empty adapter
            }
        }



    }

    private fun setupButtonListeners() = with(binding) {
//        menuButton.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        addButton.setOnClickListener {
            showUploadPopup()
        }
        imagesMoreText.setOnClickListener { navigate(R.id.action_userScreen_to_imagesFragment) }
        videosMoreText.setOnClickListener { navigate(R.id.action_userScreen_to_videoFragment) }
        audiosMoreText.setOnClickListener { navigate(R.id.action_userScreen_to_audiosFragment) }
        userProfile.setOnClickListener { navigate(R.id.action_userScreen_to_userProfileFragment) }
        userHome.setOnClickListener { navigate(R.id.action_userScreen_self) }
        var isTinted = false

        homeImage.setOnClickListener {
            if (isTinted) {
                homeImage.clearColorFilter() // Remove tint
                isTinted = false
            } else {
                homeImage.setColorFilter(ContextCompat.getColor(requireContext(), R.color.black), android.graphics.PorterDuff.Mode.SRC_IN)
                isTinted = true
            }
        }
    }

    private fun showUploadPopup() {
        if (::adapter.isInitialized) {
            adapter.releaseMediaPlayer()
        } else {
            // Initialize the adapter with an empty list or the necessary data
            adapter = AudioAdapter(emptyList())
        }
        createDialog(R.layout.popup_screen_for_upload) { dialog ->
            with(dialog) {
                setClick(R.id.btn_image) {
                    handlePermission(android.Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE) { launchCameraForImage() }
                }
                setClick(R.id.btn_video) {
                    handlePermission(android.Manifest.permission.CAMERA, CAMERA_PERMISSION_REQUEST_CODE) { launchCameraForVideo() }
                }
                setClick(R.id.btn_audio) {
                    handlePermission(android.Manifest.permission.RECORD_AUDIO, AUDIO_PERMISSION_REQUEST_CODE) { showAudioRecordingDialog() }
                }
            }
        }
    }

    private fun createDialog(layout: Int, setup: (Dialog) -> Unit) {
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(layout)
            window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setOnDismissListener { binding.drawerLayout.visibility = View.VISIBLE }
        }
        binding.drawerLayout.visibility = View.INVISIBLE
        setup(dialog)
        dialog.show()
    }

    private fun Dialog.setClick(viewId: Int, action: () -> Unit) {
        findViewById<TextView>(viewId)?.setOnClickListener {
            action()
            dismiss()
        }
    }

    private fun handlePermission(permission: String, requestCode: Int, action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
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
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AudioRecording", "Error: ${e.message}")
                }
            }
        }
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

    private fun convertToBase64(file: File): String {
        val bytes = file.readBytes() // Read file bytes
        return Base64.encodeToString(bytes, Base64.DEFAULT) // Convert to Base64 string
    }

    private fun passAudioToNextScreen(base64Audio: String) {
        val bundle = Bundle().apply {
            putString("audioBase64", base64Audio)
            putString("mediaType", "Audio")
        }
        findNavController().navigate(R.id.action_userScreen_to_mediaUploadFragment, bundle)
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
                    val videoPath = media as String  // The absolute path of the video
                    // Save the video path to SharedPreferences
                    sharedPrefs.edit().putString("videoPathKey", videoPath).apply()
                    // Pass the key in the Bundle
                    putString("pathKey", "videoPathKey")
                }

            }
        }
        findNavController().navigate(R.id.action_userScreen_to_mediaUploadFragment, bundle)
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
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("video", ".mp4", requireContext().cacheDir)
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return tempFile.absolutePath // Return the absolute path
        } catch (e: Exception) {
            Log.e("VideoSaveError", "Failed to save video: ${e.message}")
            return null
        }
    }

    override fun onStop() {
        super.onStop()
        if (::adapter.isInitialized) {
            adapter.releaseMediaPlayer()  // Release resources only if adapter is initialized
        }
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.releaseMediaPlayer()  // Release resources only if adapter is initialized
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::adapter.isInitialized) {
            adapter.releaseMediaPlayer()  // Release resources only if adapter is initialized
        }
    }
//    override fun onDestroy() {
//        super.onDestroy()
//        mediaRecorder?.release()
//        mediaRecorder = null
//        if (::adapter.isInitialized) {
//            adapter.releaseMediaPlayer()
//        }
//    }



    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> (data?.extras?.get("data") as? Bitmap)?.let { navigateToMediaUpload("Image",it)
                }

                REQUEST_VIDEO_CAPTURE -> data?.data?.let { videoUri ->
                    val videoFilePath = saveVideoToFile(videoUri) // Get the absolute path of the video
                    videoFilePath?.let { filePath ->
                        navigateToMediaUpload("Video", filePath) // Pass the absolute path to MediaUploadFragment
                    }
                }
            }
        }
    }

    private fun navigate(actionId: Int) = findNavController().navigate(actionId)

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                CAMERA_PERMISSION_REQUEST_CODE -> launchCameraForImage()
                AUDIO_PERMISSION_REQUEST_CODE -> showAudioRecordingDialog()
            }
        }
    }
}
