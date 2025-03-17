package com.example.newsworth.ui.view.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
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
    private var sendOtpDialog: AlertDialog? = null
    private var isEditing = false


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationScreenBinding.inflate(inflater, container, false)
        binding.root.setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }

        val apiService = RetrofitClient.getApiService(requireContext())
        val repository = UserManagementRepository(apiService)
        viewModel = ViewModelProvider(
            this,
            UserManagementViewModelFactory(repository)
        )[UserManagementViewModel::class.java]

        setupPasswordVisibilityToggle(binding.passwordLayout)
        setupPasswordVisibilityToggle(binding.confirmPasswordLayout)


        val creatorRoles = listOf("Citizen", "Freelancer", "Journalist")
        val userTypeOptions = listOf("NewsWorth Creator")

        val userTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            userTypeOptions
        )
        binding.userTypeDropdown.setAdapter(userTypeAdapter)



        binding.registerButton.setOnClickListener {
            if (!isInternetAvailable()) {
                showNoInternetToast()
                return@setOnClickListener
            }
            if (validateDropdownSelections() && validateFields() && validatePasswordFields()) {
                if (binding.checkbox.isChecked) {
//                    binding.registerCardView.visibility = View.GONE
                    binding.progressBar.visibility = View.VISIBLE

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
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please agree to the terms and conditions",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.userTypeDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedType = userTypeOptions[position]
            Log.d("Dropdown", "User Type selected: $selectedType")

            binding.roleDropdown.visibility = View.VISIBLE
            binding.roleDropdown.text.clear()

            val roleOptions = when (selectedType) {
                "NewsWorth Creator" -> creatorRoles
                else -> emptyList()
            }

            val roleAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                roleOptions
            )
            binding.roleDropdown.setAdapter(roleAdapter)
        }

        binding.roleDropdown.setOnItemClickListener { _, _, _, _ ->
            Log.d("Dropdown", "Role selected.")
            binding.registrationScreen.visibility = View.VISIBLE
        }
        val genderOptions = listOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            genderOptions
        )
        binding.genderDropdown.setAdapter(genderAdapter)

        binding.genderDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedGender = genderOptions[position]
            Log.d("Dropdown", "Gender selected: $selectedGender")
        }

        binding.dateDropdown.setOnClickListener {
            showDatePickerDialog()
        }

        binding.termsAndConditions.setOnClickListener { v: View? ->
            if (activity != null) {
                val intent = Intent(activity, PdfViewerActivity::class.java)
                startActivity(intent)
            }
        }
        binding.sendOtpButton.setOnClickListener {
            if (!isInternetAvailable()) {
                showNoInternetToast()
                return@setOnClickListener
            }
            if (validateDropdownSelections() && validateFields()) {
                binding.progressBar2.visibility = View.VISIBLE
                val request = RegistrationModels.SendOtpRequest(
                    first_name = binding.firstName.text.toString(),
                    mobile = binding.mobileNumber.text.toString(),
                    email = binding.emailAddress.text.toString()
                )
                viewModel.sendOtp(request)


            }
        }

        viewModel.registrationResponse.observe(viewLifecycleOwner) { response ->
            if (response.response == "success") {
                Toast.makeText(requireContext(), response.response_message, Toast.LENGTH_SHORT)
                    .show()
                binding.progressBar.visibility = View.GONE
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
            binding.progressBar2.visibility =
                View.GONE

            if (response.response == "success") {
                isEditing = false
                Toast.makeText(
                    requireContext(),
                    response.response_message ?: "Success!",
                    Toast.LENGTH_SHORT
                ).show()
                sendOtpDialog?.dismiss()
                activity?.runOnUiThread {
                    binding.passwordsLayout.visibility =View.VISIBLE
                    binding.sendOtpButton.isEnabled = false
                    binding.sendOtpButton.alpha = 0.5f
                    binding.sendOtpButton.isClickable = false
                    Log.d("OTP_Response", "Send OTP button alpha set: ${binding.sendOtpButton.alpha}")
                }
            } else if (response.response == "fail") {
                isEditing = true

                when (response.response_message) {
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
                when (response.response_message) {
                    "Invalid otp please try again" -> {
                        Toast.makeText(
                            requireContext(),
                            response.response_message ?: "Failure!",
                            Toast.LENGTH_LONG
                        ).show()
                        showSendOtpDialog(isEmailOtp = false, isMobileOtp = true)
                        binding.passwordsLayout.visibility = View.GONE
                    }
                }
                when (response.response_message) {
                    "OTP has expired, please click on resend." -> {
                        Toast.makeText(
                            requireContext(),
                            response.response_message ?: "Failure!",
                            Toast.LENGTH_LONG
                        ).show()
                        showSendOtpDialog(isEmailOtp = true, isMobileOtp = false)
                        binding.passwordsLayout.visibility = View.GONE
                    }
                }
            }
            if (response.response == "success") {
                Toast.makeText(
                    requireContext(),
                    response.response_message ?: "Success!",
                    Toast.LENGTH_SHORT
                ).show()
                sendOtpDialog?.dismiss()
                activity?.runOnUiThread {
                    binding.passwordsLayout.visibility =View.VISIBLE
                    binding.sendOtpButton.isEnabled = false
                    binding.sendOtpButton.alpha = 0.5f // Adjust as needed
                    binding.sendOtpButton.isClickable = false
                    Log.d("OTP_Response", "Send OTP button alpha set: ${binding.sendOtpButton.alpha}")
                }
            } else if (response.response == "fail") {
                isEditing = true

                // Check for both email and mobile failure
                val emailFail = response.email_response?.response == "fail"
                val mobileFail = response.mobile_response?.response == "fail"

//                if (emailFail && mobileFail) {
                    Toast.makeText(
                        requireContext(),
                        "Invalid OTPs for both email and mobile",
                        Toast.LENGTH_LONG
                    ).show()
                    showSendOtpDialog(isEmailOtp = true, isMobileOtp = true)
                    binding.passwordsLayout.visibility = View.GONE

//                }
                response.mobile_response?.let { mobileResponse ->
                    if (mobileResponse.response == "success") {
                        Toast.makeText(
                            requireContext(),
                            "Mobile Response: ${mobileResponse.response_message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        sendOtpDialog?.dismiss()
                        activity?.runOnUiThread {
                            binding.passwordsLayout.visibility =View.VISIBLE
                            binding.sendOtpButton.isEnabled = false
                            binding.sendOtpButton.alpha = 0.5f
                            binding.sendOtpButton.isClickable = false
                            Log.d("OTP_Response", "Send OTP button alpha set: ${binding.sendOtpButton.alpha}")
                        }
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

                response.email_response?.let { emailResponse ->
                    if (emailResponse.response == "success") {
                        Toast.makeText(
                            requireContext(),
                            "Email Response: ${emailResponse.response_message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        sendOtpDialog?.dismiss()
                        activity?.runOnUiThread {
                            binding.passwordsLayout.visibility =View.VISIBLE
                            binding.sendOtpButton.isEnabled = false
                            binding.sendOtpButton.alpha = 0.5f
                            binding.sendOtpButton.isClickable = false
                            Log.d("OTP_Response", "Send OTP button alpha set: ${binding.sendOtpButton.alpha}")
                        }
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
                if ((response.mobile_response?.response == "fail" || response.mobile_response?.response == "success") &&
                    (response.email_response?.response == "fail" || response.email_response?.response == "success")
                ) {
                    showSendOtpDialog(isEmailOtp = true, isMobileOtp = true)
                    binding.passwordsLayout.visibility = View.GONE
                }
            }
        }

        viewModel.otpResponse.observe(viewLifecycleOwner) { response ->
            response?.let {
                binding.progressBar2.visibility =
                    View.GONE
                handleOtpResponse(it)
            }
        }

        return binding.root
    }

    private fun handleOtpResponse(response: RegistrationModels.SendOtpResponse) {
        when (response.response) {
            "success" -> {
                binding.progressBar2.visibility = View.GONE
                handleSuccessResponse(response.response_message)
            }

            "failure" -> {
                binding.progressBar2.visibility = View.GONE
                handleFailureResponse(response.response_message)
            }

            "fail" -> {
                binding.progressBar2.visibility = View.GONE
                handleFailureResponse(response.response_message)
            }
        }
    }

    private var currentToast: Toast? = null

    private fun showToast(message: String) {
        currentToast?.cancel()
        currentToast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT)
        currentToast?.show()
    }

    private fun showNoInternetToast() {
        Toast.makeText(
            requireContext(),
            "No internet connection. Please try again later.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService<ConnectivityManager>()
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    private fun handleSuccessResponse(message: String) {
        showToast(message)

        when (message) {
            "Mobile is already registered. Please use different mobile to proceed" -> {
                binding.passwordsLayout.visibility = View.GONE
            }
            "Email is already registered. Please use different email to proceed"->{
                binding.passwordsLayout.visibility = View.GONE
            }
            "Mobile is already registered. Please complete your registration and activate your account." ->{
                activity?.runOnUiThread {
                    binding.passwordsLayout.visibility =View.VISIBLE
                    binding.sendOtpButton.isEnabled = false
                    binding.sendOtpButton.alpha = 0.5f
                    binding.sendOtpButton.isClickable = false
                    Log.d("OTP_Response", "Send OTP button alpha set: ${binding.sendOtpButton.alpha}")
                }

            }
            "Email is already registered. Please complete your registration and activate your account." ->{
                activity?.runOnUiThread {
                    binding.passwordsLayout.visibility =View.VISIBLE
                    binding.sendOtpButton.isEnabled = false
                    binding.sendOtpButton.alpha = 0.5f
                    binding.sendOtpButton.isClickable = false
                    Log.d("OTP_Response", "Send OTP button alpha set: ${binding.sendOtpButton.alpha}")
                }
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
            "Both Mobile and Email already registered. Please verify your Mobile and Email to proceed"->{
                showSendOtpDialog(isEmailOtp = true, isMobileOtp = true)
                binding.passwordsLayout.visibility = View.GONE
            }

            "Mobile is already registered. Please verify your mobile to proceed." -> {
                showSendOtpDialog(isEmailOtp = false, isMobileOtp = true)
            }

            "Please verify your email to proceed." -> {
                showSendOtpDialog(isEmailOtp = true, isMobileOtp = false)
            }

            "both Mobile and EMail  already registered.Please verify your Mobile and Email to proceed" -> {
                showSendOtpDialog(isEmailOtp = true, isMobileOtp = true)
            }

            "Please complete your registration and activate your account." -> {
                activity?.runOnUiThread {
                    binding.passwordsLayout.visibility =View.VISIBLE
                    binding.sendOtpButton.isEnabled = false
                    binding.sendOtpButton.alpha = 0.5f
                    binding.sendOtpButton.isClickable = false
                    Log.d("OTP_Response", "Send OTP button alpha set: ${binding.sendOtpButton.alpha}")
                }
            }
        }
    }

    private fun handleFailureResponse(message: String) {
        showToast(message)

        when (message) {
            "Email is already registered. Please log in or use a different email." -> {
                binding.passwordsLayout.visibility = View.GONE
            }

            "Both email and mobile number are already registered. Please use different credentials." -> {
                binding.passwordsLayout.visibility = View.GONE
            }

            "Please complete your registration and activate your account." -> {
                activity?.runOnUiThread {
                    binding.passwordsLayout.visibility =View.VISIBLE
                    binding.sendOtpButton.isEnabled = false
                    binding.sendOtpButton.alpha = 0.5f
                    binding.sendOtpButton.isClickable = false
                    Log.d("OTP_Response", "Send OTP button alpha set: ${binding.sendOtpButton.alpha}")
                }

            }

            "Please click here to complete your registration and activate your account." -> {
                activity?.runOnUiThread {
                    binding.passwordsLayout.visibility =View.VISIBLE
                    binding.sendOtpButton.isEnabled = false
                    binding.sendOtpButton.alpha = 0.5f
                    binding.sendOtpButton.isClickable = false
                    Log.d("OTP_Response", "Send OTP button alpha set: ${binding.sendOtpButton.alpha}")
                }

            }

            "Mobile is already registered. Please use a different mobile number or verify your current mobile." -> {
            }

            "Email is already registered. Please verify your email or use a different email." -> {
            }
        }
    }


    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        calendar.add(Calendar.YEAR, -18)
        val maxDate = calendar.timeInMillis

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDateCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }

                val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val formattedDateForApi = apiDateFormat.format(selectedDateCalendar.time)

                binding.dateDropdown.setText(formattedDateForApi)
                Log.d("DatePicker", "Date selected: $formattedDateForApi")
            },
            year,
            month,
            day
        ).apply {
            datePicker.maxDate = maxDate
        }.show()
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
    private fun validateFields(): Boolean {
        val firstName = binding.firstName.text.toString().trim()
        val lastName = binding.lastName.text.toString().trim()
        val mobileNumber = binding.mobileNumber.text.toString().trim()
        val emailAddress = binding.emailAddress.text.toString().trim()

        val context = requireContext()

        when {
            firstName.isBlank() -> {
                Toast.makeText(context, "First Name is required", Toast.LENGTH_SHORT).show()
                return false
            }
            !isValidName(firstName) -> {
                Toast.makeText(context, "Invalid First Name (letters only)", Toast.LENGTH_SHORT).show()
                return false
            }
            lastName.isBlank() -> {
                Toast.makeText(context, "Last Name is required", Toast.LENGTH_SHORT).show()
                return false
            }
            !isValidName(lastName) -> {
                Toast.makeText(context, "Invalid Last Name (letters only)", Toast.LENGTH_SHORT).show()
                return false
            }
            mobileNumber.isBlank() && emailAddress.isBlank() -> {
                Toast.makeText(context, "Mobile Number or Email Address is required", Toast.LENGTH_SHORT).show()
                return false
            }
            mobileNumber.isNotBlank() && !mobileNumber.matches(Regex("^\\d{10}\$")) -> {
                Toast.makeText(context, "Enter a valid 10-digit Mobile Number", Toast.LENGTH_SHORT).show()
                return false
            }
            emailAddress.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches() -> {
                Toast.makeText(context, "Enter a valid Email Address", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }
    private fun isValidName(name: String): Boolean {
        return when {
            name.isBlank() -> false
            else -> name.matches(Regex("^[a-zA-Z]+( [a-zA-Z]+)*\$"))
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

            else -> {
                val dateOfBirth = binding.dateDropdown.text.toString()
                if (dateOfBirth.isNullOrBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "Please select Date Of Birth",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    return false
                }

                val dob = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateOfBirth)
                val age = calculateAge(dob)

                if (age < 18) {
                    Toast.makeText(requireContext(), "Age must be 18 or above", Toast.LENGTH_SHORT)
                        .show()
                    return false
                }
                true
            }
        }
    }

    fun calculateAge(dob: Date?): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val birthYear = Calendar.getInstance().apply { time = dob }.get(Calendar.YEAR)
        return currentYear - birthYear
    }

    private fun setupPasswordVisibilityToggle(textInputLayout: TextInputLayout) {
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
            Toast.makeText(
                requireContext(),
                "Password and Confirm Password do not match",
                Toast.LENGTH_SHORT
            ).show()
            false
        } else {
            true
        }
    }

    private fun validatePasswordLength(password: String): Boolean {
        return if (password.length < 8) {
            Toast.makeText(
                requireContext(),
                "Password must be at least 8 characters long",
                Toast.LENGTH_SHORT
            ).show()
            false
        } else {
            true
        }
    }

    private fun validateUppercase(password: String): Boolean {
        return if (!password.any { it.isUpperCase() }) {
            Toast.makeText(
                requireContext(),
                "Password must contain at least one uppercase letter",
                Toast.LENGTH_SHORT
            ).show()
            false
        } else {
            true
        }
    }

    private fun validateLowercase(password: String): Boolean {
        return if (!password.any { it.isLowerCase() }) {
            Toast.makeText(
                requireContext(),
                "Password must contain at least one lowercase letter",
                Toast.LENGTH_SHORT
            ).show()
            false
        } else {
            true
        }
    }

    private fun validateNumber(password: String): Boolean {
        return if (!password.any { it.isDigit() }) {
            Toast.makeText(
                requireContext(),
                "Password must contain at least one number",
                Toast.LENGTH_SHORT
            ).show()
            false
        } else {
            true
        }
    }

    private fun validateSpecialCharacter(password: String): Boolean {
        val specialCharacterRegex = Regex("[!@#\$%^&*(),.?\":{}|<>]")
        return if (!specialCharacterRegex.containsMatchIn(password)) {
            Toast.makeText(
                requireContext(),
                "Password must contain at least one special character",
                Toast.LENGTH_SHORT
            ).show()
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

        dialog.show()
    }

    private fun showSendOtpDialog(isEmailOtp: Boolean, isMobileOtp: Boolean) {
        try {
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.registration_sendotp_cardview, null)

            sendOtpDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()


            val emailOtpInput = dialogView.findViewById<EditText>(R.id.etEmailOtp)
            val mobileOtpInput = dialogView.findViewById<EditText>(R.id.etMobileOtp)
            val verifyOtpButton = dialogView.findViewById<Button>(R.id.btnVerify)
            val resendOtpButton = dialogView.findViewById<Button>(R.id.btnResendOtp)
            val editDetailsButton = dialogView.findViewById<Button>(R.id.btnEditDetails)
            val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

            editDetailsButton.setOnClickListener {
                enableFields()
                isEditing = true
                sendOtpDialog?.dismiss()
            }
            cancelButton.setOnClickListener {
                sendOtpDialog?.dismiss()
            }

            emailOtpInput.visibility = if (isEmailOtp) View.VISIBLE else View.GONE
            mobileOtpInput.visibility = if (isMobileOtp) View.VISIBLE else View.GONE
            editDetailsButton.isEnabled = true
            cancelButton.isEnabled = true

            verifyOtpButton.setOnClickListener {
                if (!isInternetAvailable()) {
                    showNoInternetToast()
                    return@setOnClickListener
                }
                val emailOtp = emailOtpInput.text.toString()
                val mobileOtp = mobileOtpInput.text.toString()

                if ((isEmailOtp && emailOtp.isNotBlank() && emailOtp.length == 6) ||

                    (isMobileOtp && mobileOtp.isNotBlank() && mobileOtp.length == 6)
                ) {
                    if (isEmailOtp && emailOtp.isNotBlank() && isMobileOtp && mobileOtp.isNotBlank()) {
                        val request = RegistrationModels.SignupVerificationRequest(
                            email = binding.emailAddress.text.toString(),
                            mobile = binding.mobileNumber.text.toString(),
                            email_otp = emailOtp,
                            mobile_otp = mobileOtp
                        )

                        viewModel.verifySignup(request)

                        sendOtpDialog?.dismiss()
                        binding.passwordsLayout.visibility = View.VISIBLE
                        disableFields()

                    } else {
                        if (isEmailOtp && emailOtp.isNotBlank() && emailOtp.length == 6) {
                            val request = RegistrationModels.SignupVerificationRequest(
                                email = binding.emailAddress.text.toString(),
                                mobile = binding.mobileNumber.text.toString(),
                                email_otp = emailOtp,
                                mobile_otp = ""
                            )
                            viewModel.verifySignup(request)
                            sendOtpDialog?.dismiss()
                            binding.passwordsLayout.visibility = View.VISIBLE
                            disableFields()
                        } else if (isMobileOtp && mobileOtp.isNotBlank() && mobileOtp.length == 6) {
                            val request = RegistrationModels.SignupVerificationRequest(
                                email = binding.emailAddress.text.toString(),
                                mobile = binding.mobileNumber.text.toString(),
                                email_otp = "",
                                mobile_otp = mobileOtp
                            )
                            viewModel.verifySignup(request)
                            sendOtpDialog?.dismiss()
                            binding.passwordsLayout.visibility = View.VISIBLE
                            disableFields()
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Please enter valid 6-digit OTP/OTP'S",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            sendOtpDialog?.setOnDismissListener {
                binding.registerCardView.visibility = View.VISIBLE
                enableFields()
            }

            resendOtpButton.setOnClickListener {
                val resendRequest = RegistrationModels.SendOtpRequest(
                    first_name = binding.firstName.text.toString(),
                    mobile = binding.mobileNumber.text.toString(),
                    email = binding.emailAddress.text.toString()
                )
                viewModel.sendOtp(resendRequest)
                Toast.makeText(requireContext(), "OTP has been resent", Toast.LENGTH_SHORT).show()
            }

            editDetailsButton.setOnClickListener {
                sendOtpDialog?.dismiss()
                enableFields()
            }

            sendOtpDialog?.show()

        } catch (e: Exception) {
            Log.e("OTPDialogError", "Error in showing OTP dialog", e)
            Toast.makeText(
                requireContext(),
                "Something went wrong while loading the OTP dialog.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}