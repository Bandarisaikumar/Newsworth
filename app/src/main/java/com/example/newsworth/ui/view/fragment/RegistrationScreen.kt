package com.example.newsworth.ui.view.fragment

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.RegistrationModels
import com.example.newsworth.databinding.FragmentRegistrationScreenBinding
import com.example.newsworth.repository.UserManagementRepository
import com.example.newsworth.ui.view.PdfViewerActivity
import com.example.newsworth.ui.viewmodel.UserManagementViewModel
import com.example.newsworth.ui.viewmodel.UserManagementViewModelFactory
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class RegistrationScreen : Fragment() {

    private lateinit var binding: FragmentRegistrationScreenBinding
    private lateinit var viewModel: UserManagementViewModel
    private var sendOtpDialog: AlertDialog? = null // Store the dialog instance



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationScreenBinding.inflate(inflater, container, false)

        // Initialize ViewModel with ApiService from RetrofitClient
        val apiService = RetrofitClient.getApiService(requireContext())
        val repository = UserManagementRepository(apiService)
        viewModel = ViewModelProvider(
            this,
            UserManagementViewModelFactory(repository)
        )[UserManagementViewModel::class.java]

        // Apply the function to your password fields
        setupPasswordVisibilityToggle(binding.passwordLayout)
        setupPasswordVisibilityToggle(binding.confirmPasswordLayout) // If you have another field



        // Dropdown items for User Type and Role
        val creatorRoles = listOf("Citizen", "Freelancer", "Journalist")
        val userTypeOptions = listOf("NewsWorth Creator")

        // Set adapter for User Type dropdown
        val userTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            userTypeOptions
        )
        binding.userTypeDropdown.setAdapter(userTypeAdapter)



        binding.registerButton.setOnClickListener {
            if (validateDropdownSelections() && validateFields() && validatePasswordFields()) {
                if(binding.checkbox.isChecked) {
//                    binding.registerCardView.visibility = View.GONE
                    val request = RegistrationModels.RegistrationRequest(
                        first_name = binding.firstName.text.toString(),
                        middle_name = binding.middleName.text.toString(),
                        last_name = binding.lastName.text.toString(),
                        dob = binding.dateDropdown.text.toString(),
                        gender = binding.genderDropdown.text.toString(),
                        country = "India",
                        user_type = binding.roleDropdown.text.toString(),
                        user_email = binding.emailAddress.text.toString(),
                        user_phone_number = binding.mobileNumber.text.toString(),
                        password = binding.password.text.toString(),
                        confirm_password = binding.confirmPassword.text.toString()
                    )
                    viewModel.registerUser(request)
                }else{
                    Toast.makeText(requireContext(), "Please agree to the terms and conditions", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Handle selection of User Type
        binding.userTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedType = userTypeOptions[position]
            Log.d("Dropdown", "User Type selected: $selectedType")

            // Show Role dropdown and reset text
            binding.roleDropdown.visibility = View.VISIBLE
            binding.roleDropdown.text.clear()

            val roleOptions = when (selectedType) {
                "NewsWorth Creator" -> creatorRoles
                else -> emptyList()
            }

            // Set the adapter for Role dropdown based on the selected User Type
            val roleAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                roleOptions
            )
            binding.roleDropdown.setAdapter(roleAdapter)
        }

        // Handle selection of Role
        binding.roleDropdown.setOnItemClickListener { _, _, _, _ ->
            Log.d("Dropdown", "Role selected.")
            binding.registrationScreen.visibility = View.VISIBLE
        }
        // Gender options
        val genderOptions = listOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            genderOptions
        )
        binding.genderDropdown.setAdapter(genderAdapter)

        // Handle Gender selection
        binding.genderDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedGender = genderOptions[position]
            Log.d("Dropdown", "Gender selected: $selectedGender")
        }

        // Handle Date selection
        binding.dateDropdown.setOnClickListener {
            showDatePickerDialog()
        }

        binding.termsAndConditions.setOnClickListener { v: View? ->
            // Ensure you're in an activity context before starting the activity
            if (activity != null) {
                val intent = Intent(activity, PdfViewerActivity::class.java)
                startActivity(intent)
            }
        }
        binding.sendOtpButton.setOnClickListener {
            if (validateDropdownSelections() && validateFields()) {
                val request = RegistrationModels.SendOtpRequest(
                    first_name = binding.firstName.text.toString(),
                    mobile = binding.mobileNumber.text.toString(),
                    email = binding.emailAddress.text.toString()
                )
                viewModel.sendOtp(request)


            }
        }


        // Observe LiveData
        viewModel.registrationResponse.observe(viewLifecycleOwner) { response ->
            if (response.response == "success") {
                Toast.makeText(requireContext(), response.response_message, Toast.LENGTH_SHORT).show()

//                // Hide registration form and show success dialog
//                binding.registerCardView.visibility = View.GONE
                showSuccessDialog()
            } else {
                Toast.makeText(
                    requireContext(),
                    response.response_message,
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigate(R.id.action_registrationScreen_to_loginScreen)

            }
        }
        viewModel.verificationResponse.observe(viewLifecycleOwner) { response ->

            // Handle the main response
            if (response.response == "success") {
                Toast.makeText(
                    requireContext(),
                    response.response_message ?: "Success!",
                    Toast.LENGTH_SHORT
                ).show()
                sendOtpDialog?.dismiss() // Dismiss the dialog if verification is successful
            } else if (response.response == "fail") {
                when(response.response_message) {
                    "Invalid or incorrect OTP." -> {
                        Toast.makeText(
                            requireContext(),
                            response.response_message ?: "Failure!",
                            Toast.LENGTH_LONG
                        ).show()
                        showSendOtpDialog(isEmailOtp = true, isMobileOtp = false)
                        binding.passwordsLayout.visibility = View.GONE
                    }
                }
                when(response.response_message) {
                    "Invalid OTP, please try again." -> {
                        Toast.makeText(
                            requireContext(),
                            response.response_message ?: "Failure!",
                            Toast.LENGTH_LONG
                        ).show()
                        showSendOtpDialog(isEmailOtp = false, isMobileOtp = true)
                        binding.passwordsLayout.visibility = View.GONE
                    }
                }
            }

            // Handle the main response
            if (response.response == "success") {
                Toast.makeText(
                    requireContext(),
                    response.response_message ?: "Success!",
                    Toast.LENGTH_SHORT
                ).show()
                sendOtpDialog?.dismiss() // Dismiss the dialog if verification is successful
            } else if (response.response == "failure") {
                // Check for both email and mobile failure
                val emailFail = response.email_response?.response == "fail"
                val mobileFail = response.mobile_response?.response == "fail"

                if (emailFail && mobileFail) {
                    Toast.makeText(
                        requireContext(),
                        "Both OTPs failed. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    showSendOtpDialog(isEmailOtp = true, isMobileOtp = true)
                    binding.passwordsLayout.visibility = View.GONE
                }


                // Handle the mobile response
                response.mobile_response?.let { mobileResponse ->
                    if (mobileResponse.response == "success") {
                        Toast.makeText(
                            requireContext(),
                            "Mobile Response: ${mobileResponse.response_message}",
                            Toast.LENGTH_SHORT
                        ).show()
//                        sendOtpDialog?.dismiss() // Dismiss the dialog if verification is successful
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Mobile Response: ${mobileResponse.response_message}",
                            Toast.LENGTH_LONG
                        ).show()
                        showSendOtpDialog(isEmailOtp = true, isMobileOtp = true)
                        binding.passwordsLayout.visibility = View.GONE
                    }
                }

                // Handle the email response
                response.email_response?.let { emailResponse ->
                    if (emailResponse.response == "success") {
                        Toast.makeText(
                            requireContext(),
                            "Email Response: ${emailResponse.response_message}",
                            Toast.LENGTH_SHORT
                        ).show()
//                        sendOtpDialog?.dismiss() // Dismiss the dialog if verification is successful
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Email Response: ${emailResponse.response_message}",
                            Toast.LENGTH_LONG
                        ).show()
                        showSendOtpDialog(isEmailOtp = true, isMobileOtp = true)
                        binding.passwordsLayout.visibility = View.GONE
                    }
                }
                // If both email and mobile OTP responses failed, show the dialog
                if ((response.mobile_response?.response == "fail" || response.mobile_response?.response == "success") &&
                    (response.email_response?.response == "fail" || response.email_response?.response == "success")
                ) {
                    showSendOtpDialog(isEmailOtp = true, isMobileOtp = true)
                    binding.passwordsLayout.visibility = View.GONE
                }
                /*else if (response.response_message == "Both email and mobile Verified"){
                    sendOtpDialog?.dismiss() // Dismiss the dialog if verification is successful

                }*/
            }
        }

        viewModel.otpResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                handleOtpResponse(it)
            }
        }

        return binding.root
    }

    private fun handleOtpResponse(response: RegistrationModels.SendOtpResponse ) {
        when (response.response) {
            "success" -> {
                handleSuccessResponse(response.response_message)
            }
            "failure" -> {
                handleFailureResponse(response.response_message)
            }
        }
    }
    // Declare a global variable for the current Toast
    private var currentToast: Toast? = null

    private fun showToast(message: String) {
        // Cancel the current toast if it exists
        currentToast?.cancel()

        // Create and show a new toast
        currentToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT)
        currentToast?.show()
    }

    private fun handleSuccessResponse(message: String) {
        // Use the showToast function to display the message
        showToast(message)

        when (message) {
            "Mobile is already registered. Please log in or use a different mobile number." -> {
                binding.passwordsLayout.visibility = View.GONE
            }
            "OTP successfully sent to your email." -> {
                showSendOtpDialog(isEmailOtp = true, isMobileOtp = false)
                binding.passwordsLayout.visibility = View.GONE
            }
            "OTP successfully sent to Mobile." -> {
                showSendOtpDialog(isEmailOtp = false, isMobileOtp = true)
                binding.passwordsLayout.visibility = View.GONE
            }
            "OTP successfully sent to both email and mobile." -> {
                showSendOtpDialog(isEmailOtp = true, isMobileOtp = true)
                binding.passwordsLayout.visibility = View.GONE
            }
            "Mobile is already registered. Please verify your mobile to proceed." -> {
                showSendOtpDialog(isEmailOtp = false, isMobileOtp = true)
//                binding.passwordsLayout.visibility = View.VISIBLE
            }
            "Please verify your email to proceed." -> {
                showSendOtpDialog(isEmailOtp = true, isMobileOtp = false)
            }
            "Please complete your registration and activate your account." -> {
                binding.passwordsLayout.visibility = View.VISIBLE
            }
        }
    }

    private fun handleFailureResponse(message: String) {
        // Use the showToast function to display the message
        showToast(message)

        when (message) {
            "Email is already registered. Please log in or use a different email." -> {
                binding.passwordsLayout.visibility = View.GONE
            }
            "Both email and mobile number are already registered. Please use different credentials." -> {
                binding.passwordsLayout.visibility = View.GONE
            }
            "Please complete your registration and activate your account." -> {
                binding.passwordsLayout.visibility = View.VISIBLE
            }
            "Mobile is already registered. Please use a different Mobile Number." -> {
                // No additional UI updates
            }
            "Email is already registered. Please use a different email address." -> {
                // No additional UI updates
            }
        }
    }


    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Set the maximum date to 18 years ago
        calendar.add(Calendar.YEAR, -18)
        val maxDate = calendar.timeInMillis

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Create a Calendar instance with the selected date
                val selectedDateCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }

                // Format the selected date to "yyyy-MM-dd" for the API
                val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDateForApi = apiDateFormat.format(selectedDateCalendar.time)

                // Set the formatted date in the dateDropdown
                binding.dateDropdown.setText(formattedDateForApi)
                Log.d("DatePicker", "Date selected: $formattedDateForApi")
            },
            year,
            month,
            day
        ).apply {
            // Set the maximum date for the date picker to 18 years ago
            datePicker.maxDate = maxDate
        }.show()
    }
    private fun validateFields(): Boolean {
        // Trim whitespace from user inputs before validation
        val firstName = binding.firstName.text.toString().trim()
        val lastName = binding.lastName.text.toString().trim()
        val mobileNumber = binding.mobileNumber.text.toString().trim()
        val emailAddress = binding.emailAddress.text.toString().trim()

        when {
            !isValidName(firstName) -> {
                binding.firstName.error = "First Name is required and cannot contain numbers or special characters"
                return false
            }
            !isValidName(lastName) -> {
                binding.lastName.error = "Last Name is required and cannot contain numbers or special characters"
                return false
            }

            mobileNumber.isNotBlank() && !mobileNumber.matches(Regex("^\\d{10}\$")) -> {
                binding.mobileNumber.error = "Enter a valid 10-digit Mobile Number"
                return false
            }

            emailAddress.isNotBlank() &&
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches() -> {
                binding.emailAddress.error = "Enter a valid Email Address"
                return false
            }

            mobileNumber.isBlank() && emailAddress.isBlank() -> {
                binding.mobileNumber.error = "Mobile Number or Email Address is required"
                binding.emailAddress.error = "Mobile Number or Email Address is required"
                return false
            }
        }
        return true
    }

    // Updated helper function for validating names
    private fun isValidName(name: String): Boolean {
        return when {
            name.isBlank() -> false // Checks if the name is blank
            else -> name.matches(Regex("^[a-zA-Z]+( [a-zA-Z]+)*\$")) // Regex for valid names
        }
    }

    private fun validateDropdownSelections(): Boolean {
        return when {
            binding.userTypeDropdown.text.isNullOrBlank() -> {
                Toast.makeText(
                    requireContext(),
                    "Please select a user category",
                    Toast.LENGTH_SHORT
                ).show()
                false
            }

            binding.roleDropdown.text.isNullOrBlank() -> {
                Toast.makeText(requireContext(), "Please select a user type", Toast.LENGTH_SHORT)
                    .show()
                false
            }

            binding.genderDropdown.text.isNullOrBlank() -> {
                Toast.makeText(requireContext(), "Please select your gender", Toast.LENGTH_SHORT)
                    .show()
                false
            }

            // Date of Birth validation based on age
            else -> {
                val dateOfBirth = binding.dateDropdown.text.toString()
                if (dateOfBirth.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Please select Date Of Birth", Toast.LENGTH_SHORT)
                        .show()
                    return false
                }

                val dob = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateOfBirth)
                val age = calculateAge(dob)

                if (age < 18) {
                    Toast.makeText(requireContext(), "Age must be 18 or above", Toast.LENGTH_SHORT).show()
                    return false
                }
                true
            }
        }
    }

    // Function to calculate age from Date of Birth
    fun calculateAge(dob: Date?): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val birthYear = Calendar.getInstance().apply { time = dob }.get(Calendar.YEAR)
        return currentYear - birthYear
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

    private fun validatePasswordFields(): Boolean {
        val password = binding.password.text.toString()
        val confirmPassword = binding.confirmPassword.text.toString()

        return when {
            !validateNotEmpty(password, "Password cannot be empty") -> false
            !validateNotEmpty(confirmPassword, "Confirm Password cannot be empty") -> false
            !validatePasswordsMatch(password, confirmPassword) -> false
            !validatePasswordLength(password) -> false
            !validateUppercase(password) -> false
            !validateLowercase(password) -> false
            !validateNumber(password) -> false
            !validateSpecialCharacter(password) -> false
            else -> true
        }
    }

    private fun validateNotEmpty(input: String, errorMessage: String): Boolean {
        return if (input.isBlank()) {
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun validatePasswordsMatch(password: String, confirmPassword: String): Boolean {
        return if (password != confirmPassword) {
            Toast.makeText(requireContext(), "Password and Confirm Password do not match", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun validatePasswordLength(password: String): Boolean {
        return if (password.length < 8) {
            Toast.makeText(requireContext(), "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun validateUppercase(password: String): Boolean {
        return if (!password.any { it.isUpperCase() }) {
            Toast.makeText(requireContext(), "Password must contain at least one uppercase letter", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun validateLowercase(password: String): Boolean {
        return if (!password.any { it.isLowerCase() }) {
            Toast.makeText(requireContext(), "Password must contain at least one lowercase letter", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun validateNumber(password: String): Boolean {
        return if (!password.any { it.isDigit() }) {
            Toast.makeText(requireContext(), "Password must contain at least one number", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    // Optional: Special character validation
    private fun validateSpecialCharacter(password: String): Boolean {
        val specialCharacterRegex = Regex("[!@#\$%^&*(),.?\":{}|<>]")
        return if (!specialCharacterRegex.containsMatchIn(password)) {
            Toast.makeText(requireContext(), "Password must contain at least one special character", Toast.LENGTH_SHORT).show()
            false
        } else {
            true
        }
    }

    private fun disableFields() {
        binding.firstName.isEnabled = false
        binding.middleName.isEnabled = false
        binding.lastName.isEnabled = false
        binding.dateDropdown.isEnabled = false
        binding.genderDropdown.isEnabled = false
        binding.roleDropdown.isEnabled = false
        binding.emailAddress.isEnabled = false
        binding.mobileNumber.isEnabled = false
        binding.password.isEnabled = false
        binding.confirmPassword.isEnabled = false
        binding.userTypeDropdown.isEnabled = false
    }

    private fun enableFields() {
        binding.firstName.isEnabled = true
        binding.middleName.isEnabled = true
        binding.lastName.isEnabled = true
        binding.dateDropdown.isEnabled = true
        binding.genderDropdown.isEnabled = true
        binding.roleDropdown.isEnabled = true
        binding.emailAddress.isEnabled = true
        binding.mobileNumber.isEnabled = true
        binding.password.isEnabled = true
        binding.confirmPassword.isEnabled = true
        binding.userTypeDropdown.isEnabled = true
    }

    private fun showSuccessDialog() {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.registration_success_cardview, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        val doneBtn = dialogView.findViewById<Button>(R.id.doneButton)
        doneBtn.setOnClickListener {
            dialog.dismiss()
            findNavController().navigate(R.id.action_registrationScreen_to_loginScreen)

        }
//
//        dialog.setOnDismissListener {
//            // Make the registration card visible again when the dialog is dismissed
//            binding.registerCardView.visibility = View.VISIBLE
//        }

        dialog.show()
    }

    private fun showSendOtpDialog(isEmailOtp: Boolean, isMobileOtp: Boolean) {
        try {
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.registration_sendotp_cardview, null)

            // Create the dialog
            sendOtpDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            // Find the OTP input and buttons
            val emailOtpInput = dialogView.findViewById<EditText>(R.id.etEmailOtp)
            val mobileOtpInput = dialogView.findViewById<EditText>(R.id.etMobileOtp)
            val verifyOtpButton = dialogView.findViewById<Button>(R.id.btnVerify)
            val resendOtpButton = dialogView.findViewById<Button>(R.id.btnResendOtp)
            val editDetailsButton = dialogView.findViewById<Button>(R.id.btnEditDetails)

            // Show or hide fields based on the provided flags
            emailOtpInput.visibility = if (isEmailOtp) View.VISIBLE else View.GONE
            mobileOtpInput.visibility = if (isMobileOtp) View.VISIBLE else View.GONE

            // Handle OTP verification on button click
            verifyOtpButton.setOnClickListener {
                val emailOtp = emailOtpInput.text.toString()
                val mobileOtp = mobileOtpInput.text.toString()

                // Check if at least one OTP is provided and both are valid (6 digits)
                if ((isEmailOtp && emailOtp.isNotBlank() && emailOtp.length == 6) ||

                    (isMobileOtp && mobileOtp.isNotBlank() && mobileOtp.length == 6)) {
                    // Proceed only if both OTP fields are filled and valid
                    if (isEmailOtp && emailOtp.isNotBlank() && isMobileOtp && mobileOtp.isNotBlank()) {
                        // Both OTP fields are filled and valid
                        val request = RegistrationModels.SignupVerificationRequest(
                            email = binding.emailAddress.text.toString(),
                            mobile = binding.mobileNumber.text.toString(),
                            email_otp = emailOtp,
                            mobile_otp = mobileOtp
                        )

                        // Call the ViewModel's verifySignup function
                        viewModel.verifySignup(request)

                        // Hide dialog and disable fields
                        sendOtpDialog?.dismiss() // Close dialog after verification attempt
                        binding.passwordsLayout.visibility = View.VISIBLE
                        disableFields()

                    } else {
                        // Validate and send request for the filled OTP field
                        if (isEmailOtp && emailOtp.isNotBlank() && emailOtp.length == 6) {
                            // Only email OTP is provided
                            val request = RegistrationModels.SignupVerificationRequest(
                                email = binding.emailAddress.text.toString(),
                                mobile = binding.mobileNumber.text.toString(),
                                email_otp = emailOtp,
                                mobile_otp = "" // Empty mobile OTP
                            )
                            viewModel.verifySignup(request)
                            sendOtpDialog?.dismiss() // Close dialog after verification attempt
                            binding.passwordsLayout.visibility = View.VISIBLE
                            disableFields()
                        } else if (isMobileOtp && mobileOtp.isNotBlank() && mobileOtp.length == 6) {
                            // Only mobile OTP is provided
                            val request = RegistrationModels.SignupVerificationRequest(
                                email = binding.emailAddress.text.toString(),
                                mobile = binding.mobileNumber.text.toString(),
                                email_otp = "", // Empty email OTP
                                mobile_otp = mobileOtp
                            )
                            viewModel.verifySignup(request)
                            sendOtpDialog?.dismiss() // Close dialog after verification attempt
                            binding.passwordsLayout.visibility = View.VISIBLE
                            disableFields()
                        }
                    }
                } else {
                    // Show error if OTP fields are invalid
                    Toast.makeText(context, "Please enter valid 6-digit OTP/OTP'S", Toast.LENGTH_SHORT).show()
                }
            }

            // Handle dialog dismiss action and enable the fields again
            sendOtpDialog?.setOnDismissListener {
                binding.registerCardView.visibility = View.VISIBLE
                enableFields()
            }

            // Resend OTP functionality
            resendOtpButton.setOnClickListener {
                val resendRequest = RegistrationModels.SendOtpRequest(
                    first_name = binding.firstName.text.toString(),
                    mobile = binding.mobileNumber.text.toString(),
                    email = binding.emailAddress.text.toString()
                )
                viewModel.sendOtp(resendRequest)
                Toast.makeText(requireContext(), "OTP has been resent", Toast.LENGTH_SHORT).show()
            }

            // Button for editing details in case of errors
            editDetailsButton.setOnClickListener {
                sendOtpDialog?.dismiss()  // Dismiss the OTP dialog
                enableFields()    // Enable fields for editing
            }

            // Show the dialog
            sendOtpDialog?.show()

        } catch (e: Exception) {
            // Log the error if something goes wrong
            Log.e("OTPDialogError", "Error in showing OTP dialog", e)
            Toast.makeText(requireContext(), "Something went wrong while loading the OTP dialog.", Toast.LENGTH_LONG).show()
        }
    }
}