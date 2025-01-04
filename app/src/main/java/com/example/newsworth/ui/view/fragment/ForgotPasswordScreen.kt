package com.example.newsworth.ui.view.fragment

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Repository and ViewModel
        val apiService = context?.let { RetrofitClient.getApiService(it) }
        val repository = apiService?.let { UserManagementRepository(it) }
        val factory = repository?.let { UserManagementViewModelFactory(it) }
        viewModel =
            factory?.let { ViewModelProvider(this, it).get(UserManagementViewModel::class.java) }!!


        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val inputType: Int
            val maxLength: Int

            when (checkedId) {
                R.id.radioEmail -> {
                    inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS // Email selected
                    maxLength = Int.MAX_VALUE // No max length restriction
                }
                R.id.radioMobile -> {
                    inputType = InputType.TYPE_CLASS_PHONE // Mobile selected
                    maxLength = 10 // Restrict to 10 characters
                }
                else -> {
                    inputType = InputType.TYPE_CLASS_TEXT // Default
                    maxLength = Int.MAX_VALUE // No max length restriction
                }
            }

            // Clear the text in the EditText
            binding.emailOrMobileEditText.text.clear()

            // Set the input type for the EditText
            binding.emailOrMobileEditText.inputType = inputType

            // Set the max length filter
            val filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxLength))
            binding.emailOrMobileEditText.filters = filters
        }



        binding.forgotPasswordButton.setOnClickListener {
            // Check which radio button is selected (Email or Mobile)
            val option = view.findViewById<RadioButton>(binding.radioGroup.checkedRadioButtonId)?.text.toString()

            // Get the input from the EditText
            val input = binding.emailOrMobileEditText.text.toString()

            // Ensure the user selects an option from the radio group
            if (binding.radioGroup.checkedRadioButtonId == -1) {
                Toast.makeText(requireContext(), "Please select email or mobile option", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Ensure the input is not empty
            if (input.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter email or mobile", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate the email format if the user selects "Email"
            if (option == "Email" && !isValidEmail(input)) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate the mobile number format if the user selects "Mobile"
            if (option == "Mobile" && !isValidMobileNumber(input)) {
                Toast.makeText(requireContext(), "Please enter a valid mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // Create the request and call the view model to trigger the forgot password functionality
            val request = ForgotPasswordRequest(Forogot_option = option, email_or_mobile = input)
            viewModel.forgotPassword(request, requireContext())
            Toast.makeText(requireContext(), "Processing..", Toast.LENGTH_LONG).show()

        }



        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_forgotPasswordScreen_to_loginScreen)

        }

        viewModel.forgotPasswordResponse.observe(viewLifecycleOwner) { response ->
            if (response!= null && response.response == "success") {
                Toast.makeText(requireContext(), response.responseMessage, Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_forgotPasswordScreen_to_resetPasswordScreen)

            } else {
                if (response != null) {
                    Toast.makeText(requireContext(), "Error: ${response.responseMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    // Function to validate email format using a simple regex
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        return email.matches(Regex(emailRegex))
    }

    // Function to validate mobile number format using a simple regex
    private fun isValidMobileNumber(mobile: String): Boolean {
        val mobileRegex = "^[0-9]{10}$" // Assuming a 10-digit mobile number (adjust for your region)
        return mobile.matches(Regex(mobileRegex))
    }


}