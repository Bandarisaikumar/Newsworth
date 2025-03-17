package com.example.newsworth.ui.view.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
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
    private lateinit var binding: FragmentLoginScreenBinding
    private var selectedTab: Int = R.id.emailLoginButton


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_loginScreen_to_signinSignupScreen)
                }
            })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isUserLoggedIn()) {
            findNavController().navigate(R.id.action_loginScreen_to_homeScreen)
            return
        }
        binding.root.setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }


        val apiService = context?.let { RetrofitClient.getApiService(it) }
        val repository = apiService?.let { UserManagementRepository(it) }
        val factory = repository?.let { UserManagementViewModelFactory(it) }
        if (factory == null) {
            Toast.makeText(context, "Service unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        loginViewModel = ViewModelProvider(this, factory)[UserManagementViewModel::class.java]

        setupDefaultTab()

        loginViewModel.userId.observe(viewLifecycleOwner) { userId ->
            userId?.let {
                SharedPrefModule.provideTokenManager(requireContext()).userId = it.toString()
                println("User ID: $it")

            }
        }

        val textInputLayout = binding.loginInputLayout2
        val editText = textInputLayout.editText

        var isPasswordVisible = false

        textInputLayout.endIconMode = TextInputLayout.END_ICON_CUSTOM

        val currentFontFamily = editText?.typeface

        editText?.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        textInputLayout.endIconDrawable = ContextCompat.getDrawable(
            requireContext(),
            R.drawable.visibility_off
        )
        textInputLayout.setEndIconOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                editText?.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                textInputLayout.endIconDrawable = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.visibility_on
                )
            } else {
                editText?.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                textInputLayout.endIconDrawable = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.visibility_off
                )
            }
            editText?.typeface = currentFontFamily

            editText?.setSelection(editText.text?.length ?: 0)
        }

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginScreen_to_signinSignupScreen)
        }

        binding.signUpText.setOnClickListener {
            findNavController().navigate(R.id.action_loginScreen_to_registrationScreen)
        }
        binding.forgotPasswordBtn.setOnClickListener {
            val input = binding.loginInputEditText.text.toString().trim()

            val loginOption = when (selectedTab) {
                R.id.emailLoginButton -> "Email"
                R.id.mobileLoginButton -> "Mobile"
                R.id.useridLoginButton -> "User ID"
                else -> "Email"
            }

            if (input.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please enter your credentials",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val bundle = Bundle().apply {
                putString("input", input)
                putString("loginOption", loginOption)
            }

            findNavController().navigate(R.id.action_loginScreen_to_forgotPasswordScreen, bundle)
        }


        binding.loginButton.setOnClickListener {

            if (!isInternetAvailable()) {
                showNoInternetToast()
                return@setOnClickListener
            }

            val input = binding.loginInputEditText.text.toString().trim()
            val password = binding.loginInputEditText2.text.toString().trim()
            val isEmailLogin = selectedTab == R.id.emailLoginButton


            if (validateInput(input, password, isEmailLogin)) {
                val requestBody = createRequestBody(input, password)
                loginViewModel.getAccessToken(requestBody)
            }
        }

        binding.mobileLoginButton.setOnClickListener {
            switchToMobileTab()
            binding.mobileTab.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )

        }

        binding.emailLoginButton.setOnClickListener {
            switchToEmailTab()
            binding.emailTab.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )


        }
        binding.useridLoginButton.setOnClickListener {
            switchToUserIdTab()
            binding.useridTab.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )

        }

        observeLoginResponses()

        binding.mobileLoginButton.setOnClickListener {
            if (selectedTab != R.id.mobileLoginButton) {
                switchToMobileTab()
                selectedTab = R.id.mobileLoginButton
                updateTabBackgrounds()
            }
        }

        binding.emailLoginButton.setOnClickListener {
            if (selectedTab != R.id.emailLoginButton) {
                switchToEmailTab()
                selectedTab = R.id.emailLoginButton
                updateTabBackgrounds()
            }
        }

        binding.useridLoginButton.setOnClickListener {
            if (selectedTab != R.id.useridLoginButton) {
                switchToUserIdTab()
                selectedTab = R.id.useridLoginButton
                updateTabBackgrounds()
            }
        }

    }

    private fun isUserLoggedIn(): Boolean {
        val accessToken = SharedPrefModule.provideTokenManager(requireContext()).accessToken
        return !accessToken.isNullOrEmpty()
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


    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }


    private fun setupDefaultTab() {
        switchToEmailTab()
    }

    private fun updateTabBackgrounds() {
        binding.emailTab.setBackgroundResource(if (selectedTab == R.id.emailLoginButton) R.drawable.rectangular_button2 else R.drawable.rectangular_button)
        binding.mobileTab.setBackgroundResource(if (selectedTab == R.id.mobileLoginButton) R.drawable.rectangular_button2 else R.drawable.rectangular_button)
        binding.useridTab.setBackgroundResource(if (selectedTab == R.id.useridLoginButton) R.drawable.rectangular_button2 else R.drawable.rectangular_button)
    }

    private fun switchToMobileTab() {
        binding.loginInputLayout.hint = "Mobile*"
        binding.loginInputLayout.editText?.inputType = InputType.TYPE_CLASS_PHONE
        binding.loginInputLayout.editText?.filters = arrayOf(InputFilter.LengthFilter(10))
        binding.text.text = "Enter your mobile number and password to login"

        binding.loginInputEditText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.icon4,
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
        binding.text.text = "Enter your user id and password to login"

        binding.loginInputEditText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.icon5,
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
        binding.loginInputLayout.editText?.filters = arrayOf()
        binding.text.text = "Enter your email and password to login"
        binding.mobileTab.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.blacks
            )
        )
        binding.useridTab.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.blacks
            )
        )

        binding.loginInputEditText.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.icon1,
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
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
        } else {
            if (selectedTab == R.id.mobileLoginButton) {
                if (!input.matches("\\d{10}".toRegex())) {
                    showToast("Please enter a valid 10-digit mobile number")
                    return false
                }
            } else if (selectedTab == R.id.useridLoginButton) {
                if (!input.matches("\\d+".toRegex())) {
                    showToast("Please enter a valid User ID")
                    return false
                }
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
                if (tokenResponse.access_token != null) {
                    SharedPrefModule.provideTokenManager(requireContext()).accessToken =
                        tokenResponse.access_token
                    performLogin()
                } else {
                    val errorMessage = tokenResponse.detail ?: "Login failed. Please try again."
                    showToast(errorMessage)
                    hideProgressBarAndEnableButtons()
                }
            } else {
                showToast("Login failed. Please try again.")
                hideProgressBarAndEnableButtons()
            }
        }

        loginViewModel.loginResponse.observe(viewLifecycleOwner) { loginResponse ->
            hideProgressBarAndEnableButtons()
            if (loginResponse?.response == "success") {
                showToast(loginResponse.response_message)
                findNavController().navigate(R.id.action_loginScreen_to_homeScreen)
            } else {
                if (loginResponse?.response == "fail") {
                    showToast(loginResponse.response_message)
                }
            }
        }
    }

    private fun hideProgressBarAndEnableButtons() {
        binding.progressBar.visibility = View.GONE
        binding.loginButton.isEnabled = true
        binding.forgotPasswordBtn.isEnabled = true
        binding.signUpText.isEnabled = true
    }

    private fun performLogin() {
        binding.progressBar.visibility = View.VISIBLE
        binding.loginButton.isEnabled = false
        binding.forgotPasswordBtn.isEnabled = false
        binding.signUpText.isEnabled = false
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
