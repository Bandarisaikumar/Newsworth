package com.example.newsworth.ui.view.fragment

import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.LoginRequest
import com.example.newsworth.databinding.FragmentLoginScreenBinding
import com.example.newsworth.repository.UserManagementRepository
import com.example.newsworth.ui.viewmodel.UserManagementViewModel
import com.example.newsworth.ui.viewmodel.UserManagementViewModelFactory
import com.example.newsworth.utils.SharedPrefModule
import com.google.android.material.textfield.TextInputLayout
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody


class LoginScreen : Fragment() {

    private lateinit var loginViewModel: UserManagementViewModel
    private lateinit var binding : FragmentLoginScreenBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentLoginScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Initialize Repository and ViewModel
        val apiService = context?.let { RetrofitClient.getApiService(it) }
        val repository = apiService?.let { UserManagementRepository(it) }
        val factory = repository?.let { UserManagementViewModelFactory(it) }
        if (factory == null) {
            Toast.makeText(context, "Service unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        loginViewModel = ViewModelProvider(this, factory)[UserManagementViewModel::class.java]

        // Default to Email Tab on Screen Load
        setupDefaultTab()

        loginViewModel.userId.observe(viewLifecycleOwner) { userId ->
            userId?.let {
                // Save userId to SharedPreferences
                SharedPrefModule.provideTokenManager(requireContext()).userId = it.toString()
                println("User ID: $it")

            }
        }

        val textInputLayout = binding.loginInputLayout2
        val editText = textInputLayout.editText

// Initialize the state
        var isPasswordVisible = false // Ensure initial state matches the setup

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

        // Back Button Logic
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginScreen_to_welcomeScreen)
        }

        // Sign Up Button
        binding.signUpText.setOnClickListener {
            findNavController().navigate(R.id.action_loginScreen_to_registrationScreen)
        }

        // Forgot Password Button
        binding.forgotPasswordBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginScreen_to_forgotPasswordScreen)
        }

        // Login Button Logic
        binding.loginButton.setOnClickListener {
            val input = binding.loginInputEditText.text.toString().trim()
            val password = binding.loginInputEditText2.text.toString().trim()
            val isEmailLogin = binding.emailUnderline.visibility == View.VISIBLE

            if (validateInput(input, password, isEmailLogin)) {
                val requestBody = createRequestBody(input, password)
                loginViewModel.getAccessToken(requestBody)
            }
        }

        // Tabs Switching Logic
        binding.mobileLoginButton.setOnClickListener {
            switchToMobileTab()
        }

        binding.emailLoginButton.setOnClickListener {
            switchToEmailTab()
        }
        binding.useridLoginButton.setOnClickListener {
            switchToUserIdTab()
        }

        // Observe Token and Login Responses
        observeLoginResponses()
    }

    private fun setupDefaultTab() {
        // Default Email Tab Settings
        switchToEmailTab()
    }
    private fun switchToMobileTab() {
        binding.loginInputLayout.hint = "Mobile*"
        binding.loginInputLayout.editText?.inputType = InputType.TYPE_CLASS_PHONE
        // Set the InputFilter to restrict to 10 digits
        binding.loginInputLayout.editText?.filters = arrayOf(InputFilter.LengthFilter(10))
        binding.mobileUnderline.visibility = View.VISIBLE
        binding.emailUnderline.visibility = View.INVISIBLE
        binding.useridUnderline.visibility = View.INVISIBLE


        // Change the drawable to the mobile icon
        binding.loginInputEditText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.icon4, // Mobile icon
            0,
            0,
            0
        )

        clearInputFields()
        focusInputField(binding.loginInputEditText)
    }
    private fun switchToUserIdTab() {
        binding.loginInputLayout.hint = "User ID*"
        binding.loginInputLayout.editText?.inputType = InputType.TYPE_CLASS_NUMBER
        binding.mobileUnderline.visibility = View.INVISIBLE
        binding.emailUnderline.visibility = View.INVISIBLE
        binding.useridUnderline.visibility = View.VISIBLE

        // Change the drawable to the mobile icon
        binding.loginInputEditText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.icon5, // Mobile icon
            0,
            0,
            0
        )

        clearInputFields()
        focusInputField(binding.loginInputEditText)
    }


    private fun switchToEmailTab() {
        binding.loginInputLayout.hint = "Email*"
        binding.loginInputLayout.editText?.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        // Remove the 10-character filter set for mobile
        binding.loginInputLayout.editText?.filters = arrayOf()
        binding.emailUnderline.visibility = View.VISIBLE
        binding.mobileUnderline.visibility = View.INVISIBLE
        binding.useridUnderline.visibility = View.INVISIBLE


        // Change the drawable to the email icon
        binding.loginInputEditText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.icon1, // Email icon
            0,
            0,
            0
        )

        clearInputFields()
        focusInputField(binding.loginInputEditText)
    }


    private fun clearInputFields() {
        binding.loginInputEditText.text?.clear()
        binding.loginInputEditText2.text?.clear()
    }

    private fun focusInputField(view: View) {
        view.requestFocus()
        showKeyboard(view)
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun createRequestBody(username: String, password: String): RequestBody {
        val requestBodyString = "client_id=scz-web-portal&" +
                "client_secret=sczportalsecret&" +
                "username=$username&" +
                "password=$password&" +
                "grant_type=password&" +
                "scope=openid email profile read write"
        return requestBodyString.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
    }

    private fun validateInput(input: String, password: String, isEmailLogin: Boolean): Boolean {
        if (input.isEmpty()) {
            showToast("Please enter your credentials")
            return false
        }

        if (isEmailLogin) {
            if (!input.matches("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}".toRegex())) {
                showToast("Please enter a valid email address")
                return false
            }
        } else if (binding.mobileUnderline.visibility == View.VISIBLE) {
            if (!input.matches("\\d{10}".toRegex())) {
                showToast("Please enter a valid 10-digit mobile number")
                return false
            }
        } else if (binding.useridUnderline.visibility == View.VISIBLE) {
            if (!input.matches("\\d+".toRegex())) {
                showToast("Please enter a valid User ID")
                return false
            }
        }

        if (password.isEmpty()) {
            showToast("Please enter password")
            return false
        }

        return true
    }

    private fun observeLoginResponses() {
        loginViewModel.tokenResponse.observe(viewLifecycleOwner) { tokenResponse ->
            if (tokenResponse != null) {
                // Token response is not null, save the access token and proceed with login
                if (tokenResponse.access_token != null) {
                    SharedPrefModule.provideTokenManager(requireContext()).accessToken = tokenResponse.access_token
                    performLogin()
                } else {
                    // If the access token is null, show the message from the `detail` field
                    val errorMessage = tokenResponse.detail ?: "Login failed. Please try again."
                    showToast(errorMessage)
                }
            } else {
                // Show a generic error message if the token response is null
                showToast("Login failed. Please try again.")
            }
        }

        loginViewModel.loginResponse.observe(viewLifecycleOwner) { loginResponse ->
            if (loginResponse?.response == "success") {
                showToast(loginResponse.response_message)
                findNavController().navigate(R.id.action_loginScreen_to_userScreen)
            } else {
                if (loginResponse?.response == "fail") {
                    showToast(loginResponse.response_message)
                }
            }
        }
    }

    private fun performLogin() {
        val input = binding.loginInputEditText.text.toString().trim()
        val password = binding.loginInputEditText2.text.toString().trim()
        val loginOption = when {
            input.contains("@") -> "Email"
            input.matches("\\d{10}".toRegex()) -> "Mobile"
            else -> "User Id"
        }
        val loginRequest = LoginRequest(
            login_option = loginOption,
            platform = "Web",
            email_or_mobile_or_user_id = input,
            password = password
        )

        loginViewModel.login(loginRequest)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

}
