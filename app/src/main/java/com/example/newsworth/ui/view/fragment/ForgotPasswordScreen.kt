package com.example.newsworth.ui.view.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.ForgotPasswordRequest
import com.example.newsworth.databinding.FragmentForgotPasswordScreenBinding
import com.example.newsworth.repository.UserManagementRepository
import com.example.newsworth.ui.viewmodel.UserManagementViewModel
import com.example.newsworth.ui.viewmodel.UserManagementViewModelFactory

class ForgotPasswordScreen : Fragment() {

    private lateinit var binding: FragmentForgotPasswordScreenBinding
    private lateinit var viewModel: UserManagementViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = FragmentForgotPasswordScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_forgotPasswordScreen_to_loginScreen)
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

        val apiService = context?.let { RetrofitClient.getApiService(it) }
        val repository = apiService?.let { UserManagementRepository(it) }
        val factory = repository?.let { UserManagementViewModelFactory(it) }
        viewModel = factory?.let { ViewModelProvider(this, it).get(UserManagementViewModel::class.java) }!!

        val input = arguments?.getString("input") ?: ""
        val loginOption = arguments?.getString("loginOption") ?: "Email"

        binding.emailOrMobileEditText.setText(input)

        binding.resetUsingTextView.text = when (loginOption) {
            "Email" -> "Don’t worry! It happens.Please enter the email associated with your account."
            "Mobile" -> "Don’t worry! It happens.Please enter the mobile number associated with your account."
            else -> "Don’t worry! It happens.Please enter the user id associated with your account."
        }
        binding.login.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordScreen_to_loginScreen)

        }

        binding.forgotPasswordButton.setOnClickListener {
            if (!isInternetAvailable()) {
                showNoInternetToast()
                return@setOnClickListener
            }
            val inputText = binding.emailOrMobileEditText.text.toString()

            if (inputText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter email or mobile", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (loginOption == "Email" && !isValidEmail(inputText)) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else if (loginOption == "Mobile" && !isValidMobileNumber(inputText)) {
                Toast.makeText(requireContext(), "Please enter a valid mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = ForgotPasswordRequest(Forogot_option = loginOption, email_or_mobile = inputText)
            binding.progressBar.visibility = View.VISIBLE
            binding.forgotPasswordButton.isEnabled = false
            viewModel.forgotPassword(request, requireContext())
        }

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordScreen_to_loginScreen)
        }

        viewModel.forgotPasswordResponse.observe(viewLifecycleOwner) { response ->
            binding.progressBar.visibility = View.GONE
            binding.forgotPasswordButton.isEnabled = true


            if (response != null && response.response == "success") {
                Toast.makeText(requireContext(), response.responseMessage, Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_forgotPasswordScreen_to_resetPasswordScreen)
            } else {
                if (response != null) {
                    Toast.makeText(requireContext(), "Error: ${response.responseMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun showNoInternetToast() {
        Toast.makeText(requireContext(), "No internet connection. Please try again later.", Toast.LENGTH_SHORT).show()
    }
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return email.matches(Regex(emailRegex))
    }

    private fun isValidMobileNumber(mobile: String): Boolean {
        val mobileRegex = "^[0-9]{10}$"
        return mobile.matches(Regex(mobileRegex))
    }
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }


}
