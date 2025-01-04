package com.example.newsworth.ui.view.fragment

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UserProfile : Fragment() {

    private lateinit var binding: FragmentUserProfileBinding
    private lateinit var profileViewModel: ProfileManagementViewmodel
    private lateinit var userViewModel: UserManagementViewModel


    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadImage(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        val isTinted = false


        val profileRepository =
            ProfileManagementRepository(RetrofitClient.getApiService(requireContext()))
        val profileFactory = ProfileManagementViewmodelFactory(profileRepository)
        profileViewModel =
            ViewModelProvider(this, profileFactory).get(ProfileManagementViewmodel::class.java)

        val userRepository =
            UserManagementRepository(RetrofitClient.getApiService(requireContext()))
        val userFactory = UserManagementViewModelFactory(userRepository)
        userViewModel =
            ViewModelProvider(this, userFactory).get(UserManagementViewModel::class.java)




        binding.addButton.setOnClickListener { navigateToUserScreenForUpload() }
        binding.userHome.setOnClickListener { findNavController().navigate(R.id.action_userProfileFragment_to_userScreen) }
        binding.backButton.setOnClickListener { findNavController().navigate(R.id.action_userProfileFragment_to_userScreen) }
        binding.logout.setOnClickListener {
            val userId =
                SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
            performLogout(userId)

        }
        binding.profileInfo.setOnClickListener {
            findNavController().navigate(R.id.action_userProfileFragment_to_profileDetailsScreen)
        }

        binding.cameraIcon.setOnClickListener {
            openGallery()
        }
        binding.myJournals.setOnClickListener {
            findNavController().navigate(R.id.action_userProfileFragment_to_userScreen)

        }
        if (isTinted) {
            binding.personImage.clearColorFilter() // Remove tint
        } else {
            binding.personImage.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.mehrun_color
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        // Fetch profile image for the user
        fetchProfileImage()

        observeViewModels()

        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
        if (userId != -1) {
            profileViewModel.fetchProfileDetails(userId) // Fetch data when the fragment is created
        } else {
            Toast.makeText(context, "User ID not available", Toast.LENGTH_SHORT).show()
        }

        profileViewModel.profileDetails.observe(viewLifecycleOwner) {  response ->
            context?.let {
                Toast.makeText(context, response.response, Toast.LENGTH_SHORT).show()

                // Bind the response data to the UI elements
                binding.apply {
               // Concatenate first name, middle name, and last name
                    val fullName = buildString {
                        append(response.response_message.first_name)
                        if (!response.response_message.middle_name.isNullOrEmpty()) {
                            append(" ${response.response_message.middle_name}")
                        }
                        if (response.response_message.last_name.isNotEmpty()) {
                            append(" ${response.response_message.last_name}")
                        }
                    }
                    // Set "User Name" and the full name to the userName TextView
                    userName.text = "User Name : ${fullName.ifEmpty { "N/A" }}"

                    // Set "User ID" and the user ID to the usersId TextView
                    usersId.text = "User ID : ${response.response_message.user_id}"

                }
            }
        }


        // Trigger the dialog when "Change Password" section is clicked
        binding.changePasswordBtn.setOnClickListener {
            showChangePasswordDialog()
        }


        return binding.root
    }

    private fun navigateToUserScreenForUpload() {
        val bundle = Bundle().apply {
            putBoolean("continueUpload", true) // Flag to continue the upload process
        }
        findNavController().navigate(R.id.action_userProfileFragment_to_userScreen, bundle)
    }

    private fun performLogout(userId: Int) {
        userViewModel.logoutUser(userId)
    }

    private fun openGallery() {
        selectImageLauncher.launch("image/*")
    }

    private fun uploadImage(uri: Uri) {
        try {
            // Display the selected image immediately
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.avthar_image2) // Optional placeholder
                .into(binding.profileImage)

            // Proceed with the upload
            val file = createFileFromUri(uri)
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartFile = MultipartBody.Part.createFormData("file", file.name, requestBody)

            val userId =
                SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
            profileViewModel.uploadProfileImage(userId, multipartFile)

            // Fetch the latest profile image URL after the upload completes
            profileViewModel.uploadResponse.observe(viewLifecycleOwner) { response ->
                if (response != null && response.response == "success") {
                    fetchProfileImage() // Fetch the updated image URL
                    Toast.makeText(
                        requireContext(),
                        "Image uploaded successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Image upload failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to process the file: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
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
//        profileViewModel.uploadResponse.observe(viewLifecycleOwner) { response ->
//            if (response != null) {
//                Toast.makeText(requireContext(), response.response, Toast.LENGTH_LONG).show()
//            } else {
//                Toast.makeText(requireContext(), "Upload failed. Please try again.", Toast.LENGTH_SHORT).show()
//            }
//        }

        profileViewModel.imageUrl.observe(viewLifecycleOwner) { imageUrl ->
            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.avthar_image2) // Add a placeholder image
                    .into(binding.profileImage)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to load profile image.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        profileViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }

        userViewModel.logoutResponse.observe(viewLifecycleOwner) { response ->
            Toast.makeText(context, response.response_message, Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_userProfileFragment_to_welcomeScreen)
        }

        userViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
        profileViewModel.changePasswordResponse.observe(viewLifecycleOwner) { response ->
            if (response.response == "success") {
                Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), response.response_message, Toast.LENGTH_LONG).show()
                showChangePasswordDialog()
            }
        }

        profileViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
    private fun showChangePasswordDialog() {

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)

        val oldPasswordEditText = dialogView.findViewById<TextInputEditText>(R.id.oldPasswordEditText)
        val newPasswordEditText = dialogView.findViewById<TextInputEditText>(R.id.newPasswordEditText)
        val confirmPasswordEditText = dialogView.findViewById<TextInputEditText>(R.id.confirmPasswordEditText)
        val changePasswordButton = dialogView.findViewById<MaterialButton>(R.id.changePasswordButton)

        // Apply password visibility toggle on each password field
        setupPasswordVisibilityToggle(dialogView.findViewById(R.id.oldPasswordEditTextLayout))
        setupPasswordVisibilityToggle(dialogView.findViewById(R.id.newPasswordEditTextLayout))
        setupPasswordVisibilityToggle(dialogView.findViewById(R.id.confirmPasswordEditTextLayout))


        // Apply the function to your password fields
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        changePasswordButton.setOnClickListener {
            val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            // Validation for empty fields
            if (oldPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Old password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.isEmpty()) {
                Toast.makeText(requireContext(), "New password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Confirm password cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validation for password length
            if (newPassword.length < 8) {
                Toast.makeText(requireContext(), "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validation for at least one uppercase letter
            if (!newPassword.any { it.isUpperCase() }) {
                Toast.makeText(requireContext(), "Password must contain at least one uppercase letter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validation for at least one lowercase letter
            if (!newPassword.any { it.isLowerCase() }) {
                Toast.makeText(requireContext(), "Password must contain at least one lowercase letter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validation for at least one number
            if (!newPassword.any { it.isDigit() }) {
                Toast.makeText(requireContext(), "Password must contain at least one number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validation for at least one special character
            if (!newPassword.any { it in "!@#$%^&*()-_=+[]{}|;:'\",.<>?/\\`~" }) {
                Toast.makeText(requireContext(), "Password must contain at least one special character", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validation for matching passwords
            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "New password and confirm password do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Proceed with the change password request
            val request = ChangePasswordRequest(
                user_id = userId,
                old_password = oldPassword,
                new_password = newPassword,
                confirm_password = confirmPassword
            )

            profileViewModel.changePassword(request)

            dialog.dismiss()
        }


        dialog.show()
    }
    private fun setupPasswordVisibilityToggle(textInputLayout: TextInputLayout) {
        val editText = textInputLayout.editText

        // Initialize the state
        var isPasswordVisible = false

        // Ensure end icon mode is custom
        textInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM

        // Save the current font family
        val currentFontFamily = editText?.typeface

        // Set the initial input type and icon
        editText?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        textInputLayout.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.visibility_off) // Closed-eye icon initially

        // Reverse the default toggle behavior
        textInputLayout.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                // Password is visible, eye icon open
                editText?.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                textInputLayout.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.visibility_on) // Open-eye icon
            } else {
                // Password is hidden, eye icon closed
                editText?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                textInputLayout.endIconDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.visibility_off) // Closed-eye icon
            }
            // Restore the font family
            editText?.typeface = currentFontFamily

            // Preserve the cursor position
            editText?.setSelection(editText.text?.length ?: 0)
        }
    }





}
