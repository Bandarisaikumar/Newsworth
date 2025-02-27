package com.example.newsworth.ui.view.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.ResetPasswordRequest
import com.example.newsworth.databinding.FragmentResetPasswordScreenBinding
import com.example.newsworth.repository.UserManagementRepository
import com.example.newsworth.ui.viewmodel.UserManagementViewModel
import com.example.newsworth.ui.viewmodel.UserManagementViewModelFactory
import com.example.newsworth.utils.SharedPrefModule
import com.google.android.material.textfield.TextInputLayout


class ResetPasswordScreen : Fragment() {

    private lateinit var binding: FragmentResetPasswordScreenBinding
    private lateinit var viewModel: UserManagementViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentResetPasswordScreenBinding.inflate(inflater, container, false)
        return binding.root    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_resetPasswordScreen_to_forgotPasswordScreen)
            }
        })
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }

        // Initialize Repository and ViewModel
        val apiService = context?.let { RetrofitClient.getApiService(it) }
        val repository = apiService?.let { UserManagementRepository(it) }
        val factory = repository?.let { UserManagementViewModelFactory(it) }
        viewModel = factory?.let { ViewModelProvider(this, it).get(UserManagementViewModel::class.java) }!!

        binding.submitButton.setOnClickListener {
            val userId = SharedPrefModule.provideTokenManager(requireContext()).userId2?.toInt() ?: -1
            val otp = binding.otpEditText.text.toString()
            val newPassword = binding.newPasswordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if (otp.isBlank()) {
                Toast.makeText(requireContext(), "Please enter OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (otp.length != 6) {
                Toast.makeText(requireContext(), "Please enter a 6-digit valid OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.isBlank()) {
                Toast.makeText(requireContext(), "Please enter a new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (confirmPassword.isBlank()) {
                Toast.makeText(requireContext(), "Please enter the confirm password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val passwordValidationError = validatePassword(newPassword)
            if (passwordValidationError != null) {
                Toast.makeText(requireContext(), passwordValidationError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = ResetPasswordRequest(
                user_id = userId,
                otp = otp,
                new_password = newPassword,
                confirm_new_password = confirmPassword
            )
            viewModel.resetPassword(request)
        }



        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_resetPasswordScreen_to_forgotPasswordScreen)

        }

        setupPasswordVisibilityToggle(binding.newPasswordEditTextLayout)
        setupPasswordVisibilityToggle(binding.confirmPasswordEditTextLayout)

        viewModel.resetPasswordResponse.observe(viewLifecycleOwner) { response ->
            if (response != null && response.body()?.response == "success") {
                Toast.makeText(requireContext(), response.body()?.response_message, Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_resetPasswordScreen_to_loginScreen)

            } else {
                Toast.makeText(requireContext(), "Error: ${response.body()?.response_message}", Toast.LENGTH_SHORT).show()
            }
        }

}

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }

    private fun validatePassword(password: String): String? {
        if (password.length < 8) {
            return "Password must be at least 8 characters long"
        }
        if (!password.any { it.isUpperCase() }) {
            return "Password must contain at least one uppercase letter"
        }
        if (!password.any { it.isLowerCase() }) {
            return "Password must contain at least one lowercase letter"
        }
        if (!password.any { it.isDigit() }) {
            return "Password must contain at least one number"
        }
        // Uncomment below if you want to enforce special characters
         if (!password.any { "!@#$%^&*(),.?\":{}|<>".contains(it) }) {
             return "Password must contain at least one special character"
         }
        return null // Valid password
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