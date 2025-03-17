package com.example.newsworth.ui.view.fragment

import android.app.AlertDialog
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.ChangePasswordRequest
import com.example.newsworth.databinding.FragmentUserProfileBinding
import com.example.newsworth.repository.ProfileManagementRepository
import com.example.newsworth.repository.UserManagementRepository
import com.example.newsworth.ui.viewmodel.ProfileManagementViewmodel
import com.example.newsworth.ui.viewmodel.ProfileManagementViewmodelFactory
import com.example.newsworth.ui.viewmodel.UserManagementViewModel
import com.example.newsworth.ui.viewmodel.UserManagementViewModelFactory
import com.example.newsworth.utils.SharedPrefModule
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.net.SocketException
import javax.net.ssl.SSLHandshakeException

class UserProfile : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private var _profileViewModel: ProfileManagementViewmodel? = null
    private val profileViewModel get() = _profileViewModel!!
    private lateinit var userViewModel: UserManagementViewModel

    private lateinit var selectImageLauncher: ActivityResultLauncher<String>

    private var isInternetAvailable: Boolean = true
    private var isViewCreated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeScreen = parentFragment as? HomeScreen
                homeScreen?.showHomeContentTab()
            }
        })
        selectImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let { uploadImage(it) }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        isViewCreated = true

        val profileRepository =
            ProfileManagementRepository(RetrofitClient.getApiService(requireContext()))
        val profileFactory = ProfileManagementViewmodelFactory(profileRepository)
        _profileViewModel =
            ViewModelProvider(this, profileFactory)[ProfileManagementViewmodel::class.java]

        val userRepository =
            UserManagementRepository(RetrofitClient.getApiService(requireContext()))
        val userFactory = UserManagementViewModelFactory(userRepository)
        userViewModel =
            ViewModelProvider(this, userFactory)[UserManagementViewModel::class.java]

        checkInternetAndSetup()


        binding.logout.setOnClickListener {
            handleButtonClick {
                val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
                performLogout(userId)

            }
        }
        binding.profileInfo.setOnClickListener {
            handleButtonClick {
                val homeScreen = parentFragment as? HomeScreen
                homeScreen?.navigateProfileDetailsFragment()
            }
        }
        binding.aboutApp.setOnClickListener {
            handleButtonClick {
                val homeScreen = parentFragment as? HomeScreen
                homeScreen?.navigateAboutAppFragment()
            }
        }
        binding.helpAndSupport.setOnClickListener {
            handleButtonClick {
                val homeScreen = parentFragment as? HomeScreen
                homeScreen?.navigateHelpAndSupport()
            }
        }

        binding.cameraIcon.setOnClickListener {
            handleButtonClick {
                openGallery()
            }
        }
        binding.changePasswordBtn.setOnClickListener {
            handleButtonClick {
                showChangePasswordDialog()
            }
        }
        binding.myJournals.setOnClickListener {
            val homeScreenFragment = parentFragment as? HomeScreen
            homeScreenFragment?.showMyFilesTab()
        }
        binding.settingsButton.setOnClickListener {
            val homeScreenFragment = parentFragment as? HomeScreen
            homeScreenFragment?.showHomeContentTabAndSettingsDialog()
        }
        fetchProfileImage()
        observeViewModels()
        _profileViewModel!!.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                _profileViewModel!!.clearErrorMessage()
            }
        }
        _profileViewModel!!.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        return binding.root
    }

    private fun checkInternetAndSetup() {
        isInternetAvailable = isInternetAvailable()
        if (isInternetAvailable) {
            fetchProfileData()
            enableUI()
        } else {
            showNoInternetDialog()
        }
    }
    private fun fetchProfileData() {
        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toIntOrNull() ?: -1
        if (userId != -1) {
            _profileViewModel?.viewModelScope?.launch {
                try {
                    showProgressBar()
                    profileViewModel.fetchProfileDetails(userId)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val errorMessage = when (e) {
                            is IOException -> {
                                when (e) {
                                    is SSLHandshakeException -> "Secure connection failed. Please try again later."
                                    is SocketException -> "Unable to connect to the server. Please check your internet connection."
                                    is EOFException -> "The connection was closed unexpectedly. Please try again later." // Handle EOFException
                                    else -> "A network error occurred. Please check your internet connection."
                                }
                            }
                            else -> "An unexpected error occurred: ${e.message}"
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                        disableUI()
                    }
                    Log.e("UserProfile", "Error fetching profile data: ${e.message}", e)
                }finally {
                    hideProgressBar()
                }
            }
        } else {
            Toast.makeText(context, "User ID not available", Toast.LENGTH_SHORT).show()
        }
    }
    private fun handleButtonClick(action: () -> Unit) {
        if (isInternetAvailable) {
            action()
        } else {
            showNoInternetToast()
        }
    }
    private fun showNoInternetToast() {
        Toast.makeText(requireContext(), "No internet connection.please turn on internet", Toast.LENGTH_SHORT).show()
    }
    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }
    private fun enableUI() {
        binding.logout.isEnabled = true
        binding.profileInfo.isEnabled = true
        binding.cameraIcon.isEnabled = true
        binding.myJournals.isEnabled = true
        binding.changePasswordBtn.isEnabled = true
    }

    private fun disableUI() {
        binding.logout.isEnabled = false
        binding.profileInfo.isEnabled = false
        binding.cameraIcon.isEnabled = false
        binding.myJournals.isEnabled = false
        binding.changePasswordBtn.isEnabled = false
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    private fun showNoInternetDialog() {
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("No Internet Connection")
            .setMessage("Please turn on your internet connection to continue.")
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> }
        val alert = builder.create()
        alert.show()
    }

    private fun performLogout(userId: Int) {
        userViewModel.logoutUser(userId)
    }

    private fun openGallery() {
        selectImageLauncher.launch("image/*")
    }

    private fun uploadImage(uri: Uri) {
        try {
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.avthar_image2)
                .into(binding.profileImage)

            val file = createFileFromUri(uri)
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartFile = MultipartBody.Part.createFormData("file", file.name, requestBody)

            val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
            profileViewModel.uploadProfileImage(userId, multipartFile)
            showProgressBar()

            profileViewModel.uploadResponse.observe(viewLifecycleOwner) { response ->
                hideProgressBar()
                if (response != null && response.response == "success") {
                    fetchProfileImage()
                    Toast.makeText(requireContext(), "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Image upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to process the file: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("UserProfile", "File processing error: ${e.message}")
            hideProgressBar()
        }
    }

    private fun createFileFromUri(uri: Uri): File {
        val contentResolver = requireContext().contentResolver
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Unable to open InputStream for URI: $uri")

        val file = File(requireContext().cacheDir, getFileNameFromUri(uri))
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "temp_image"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                fileName =
                    it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                        ?: "temp_image"
            }
        }
        return fileName
    }

    private fun fetchProfileImage() {
        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toString() ?: ""
        if (userId.isNotEmpty()) {
            profileViewModel.fetchImageLink(userId)
        } else {
            Toast.makeText(requireContext(), "User ID not found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModels() {
        profileViewModel.imageUrl.observe(viewLifecycleOwner) { imageUrlResponse ->
            val imageUrlString = imageUrlResponse?.url

            val uri: Uri? = imageUrlString?.let { Uri.parse(it) }

            if (uri != null) {
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.avthar_image2)
                    .into(binding.profileImage)
            } else {
                binding.profileImage.setImageResource(R.drawable.avthar_image2)
                Log.d("ProfileImage", "Image URL is null or invalid.")
            }
        }

        profileViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.d("ProfileViewModelError", "Error: $it") // Log the error
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                profileViewModel.clearErrorMessage()
            }
        }

        userViewModel.logoutResponse.observe(viewLifecycleOwner) { response ->
            if (response != null && response.response == "success") {
                Toast.makeText(context, response.response_message, Toast.LENGTH_LONG).show()
                SharedPrefModule.provideTokenManager(requireContext()).clearTokens()
                findNavController().navigate(R.id.action_homeScreen_to_welcomeScreen)
            } else {
                Toast.makeText(context, "Logout failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        userViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.d("UserViewModelError", "Error: $it")
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                userViewModel.clearErrorMessage()
            }
        }

        profileViewModel.changePasswordResponse.observe(viewLifecycleOwner) { response ->
            hideProgressBar()
            if (response.response == "success") {
                Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), response.response_message, Toast.LENGTH_LONG).show()
                showChangePasswordDialog()
            }
        }


        profileViewModel.profileDetails.observe(viewLifecycleOwner) { response ->
            hideProgressBar()
            context?.let {
                val fullName = buildString {
                    if (response != null) {
                        append(response.response_message.first_name)
                        if (!response.response_message.middle_name.isNullOrEmpty()) {
                            append(" ${response.response_message.middle_name}")
                        }
                        if (response.response_message.last_name.isNotEmpty()) {
                            append(" ${response.response_message.last_name}")
                        }
                    }
                }
                binding.userName.text = "User Name : ${fullName.ifEmpty { " " }}"
                if (response != null) {
                    binding.usersId.text = "User ID : ${response.response_message.user_id}"
                }
            }
        }

        profileViewModel.uploadResponse.observe(viewLifecycleOwner) { response ->
            hideProgressBar()
            if (response != null && response.response == "success") {
                fetchProfileImage()
                Toast.makeText(requireContext(), "Image uploaded successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Image upload failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        profileViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.d("ProfileImageUploadError", "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                profileViewModel.clearErrorMessage()
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)

        val oldPasswordEditText =
            dialogView.findViewById<TextInputEditText>(R.id.oldPasswordEditText)
        val newPasswordEditText =
            dialogView.findViewById<TextInputEditText>(R.id.newPasswordEditText)
        val confirmPasswordEditText =
            dialogView.findViewById<TextInputEditText>(R.id.confirmPasswordEditText)
        val changePasswordButton =
            dialogView.findViewById<MaterialButton>(R.id.changePasswordButton)


        setupPasswordVisibilityToggle(dialogView.findViewById(R.id.oldPasswordEditTextLayout))
        setupPasswordVisibilityToggle(dialogView.findViewById(R.id.newPasswordEditTextLayout))
        setupPasswordVisibilityToggle(dialogView.findViewById(R.id.confirmPasswordEditTextLayout))


        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        changePasswordButton.setOnClickListener {

            val userId =
                SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (oldPassword.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Old password cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener                        }
            if (newPassword.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "New password cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener                        }
            if (confirmPassword.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Confirm password cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener                        }

            if (newPassword.length < 8) {
                Toast.makeText(
                    requireContext(),
                    "Password must be at least 8 characters long",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener                        }

            if (!newPassword.any { it.isUpperCase() }) {
                Toast.makeText(
                    requireContext(),
                    "Password must contain at least one uppercase letter",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener                        }

            if (!newPassword.any { it.isLowerCase() }) {
                Toast.makeText(
                    requireContext(),
                    "Password must contain at least one lowercase letter",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener                        }

            if (!newPassword.any { it.isDigit() }) {
                Toast.makeText(
                    requireContext(),
                    "Password must contain at least one number",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener                        }

            if (!newPassword.any { it in "!@#$%^&*()-_=+[]{}|;:'\",.<>?/\\`~" }) {
                Toast.makeText(
                    requireContext(),
                    "Password must contain at least one special character",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener                        }

            if (newPassword != confirmPassword) {
                Toast.makeText(
                    requireContext(),
                    "New password and confirm password do not match",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener                        }

            val request = ChangePasswordRequest(
                user_id = userId,
                old_password = oldPassword,
                new_password = newPassword,
                confirm_password = confirmPassword
            )

            profileViewModel.changePassword(request)
            showProgressBar()

            dialog.dismiss()
        }

        dialog.show()
    }


    private fun setupPasswordVisibilityToggle(textInputLayout: TextInputLayout) {
        val editText = textInputLayout.editText

        var isPasswordVisible = false

        textInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM

        val currentFontFamily = editText?.typeface

        editText?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        textInputLayout.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.visibility_off)

        textInputLayout.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                editText?.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                textInputLayout.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.visibility_on)
            } else {
                editText?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                textInputLayout.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.visibility_off)
            }
            editText?.typeface = currentFontFamily
            editText?.setSelection(editText.text?.length ?: 0)
        }
    }


    override fun onResume() {
        super.onResume()
        checkInternetAndSetup()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _profileViewModel = null
        isViewCreated = false
    }
}