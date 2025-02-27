package com.example.newsworth.ui.view.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Filter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.data.model.EditProfileRequest
import com.example.newsworth.data.model.LocationDetail
import com.example.newsworth.databinding.FragmentEditProfileScreenBinding
import com.example.newsworth.repository.ProfileManagementRepository
import com.example.newsworth.ui.viewmodel.ProfileManagementViewmodel
import com.example.newsworth.ui.viewmodel.ProfileManagementViewmodelFactory
import com.example.newsworth.utils.SharedPrefModule
import java.text.SimpleDateFormat
import java.util.*

class EditProfileScreen : Fragment() {

    private lateinit var viewModel: ProfileManagementViewmodel
    private lateinit var binding: FragmentEditProfileScreenBinding
    private var isInternetAvailable: Boolean = true
    private var isViewCreated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeScreen = parentFragment as? HomeScreen
                homeScreen?.navigateProfileDetailsFragment()
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditProfileScreenBinding.inflate(inflater, container, false)
        isViewCreated = true

        binding.root.setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }

        val profileRepository = ProfileManagementRepository(RetrofitClient.getApiService(requireContext()))
        val profileFactory = ProfileManagementViewmodelFactory(profileRepository)
        viewModel = ViewModelProvider(this, profileFactory)[ProfileManagementViewmodel::class.java]

        checkInternetAndSetup() // Initial internet check

        binding.searchIcon.setOnClickListener {
            val pincode = binding.pincodeValue.text.toString()
            if (pincode.length == 6) {
                binding.locationNameValue.text.clear()
                disableLocationFields()
                viewModel.fetchLocationDetails(pincode.toInt())
            } else {
                Toast.makeText(requireContext(), "Enter a valid pincode", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.locationDetails.observe(viewLifecycleOwner) { locationList ->
            if (locationList.isNotEmpty()) {
                val location = locationList[0]
                binding.stateNameValue.setText(location.state ?: "")
                binding.districtNameValue.setText(location.district ?: "")
                binding.countryValue.setText(location.country ?: "")
            } else {
                Toast.makeText(requireContext(), "Location details not found", Toast.LENGTH_SHORT).show()
                disableLocationFields()
            }
        }

        val genderDropdown: AutoCompleteTextView = binding.genderValue
        val genderOptions = listOf("Male", "Female", "Other")
        val genderAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            genderOptions
        ) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        results.values = genderOptions
                        results.count = genderOptions.size
                        return results
                    }

                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        genderDropdown.setAdapter(genderAdapter)
        genderDropdown.setOnClickListener {
            genderDropdown.dismissDropDown()
            genderDropdown.setAdapter(genderAdapter)
            genderDropdown.showDropDown()
        }

        genderDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedGender = genderOptions[position]
        }

        val dobField: AutoCompleteTextView = binding.dobData
        dobField.setOnClickListener {
            showDatePicker(dobField)
        }

        viewModel.locationDetails.observe(viewLifecycleOwner) { locationList ->
            populateDropdowns(locationList)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
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
            disableUI()
        }
    }

    private fun fetchProfileData() {
        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
        if (userId != -1) {
            viewModel.fetchProfileDetails(userId)
        } else {
            Toast.makeText(context, "User ID not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableUI() {
        binding.saveButton.isEnabled = true
        binding.backButton.isEnabled = true
        binding.firstNameValue.isEnabled = true
        binding.middleNameValue.isEnabled = true
        binding.lastNameValue.isEnabled = true
        binding.emailValue.isEnabled = true
        binding.mobileValue.isEnabled = true
        binding.genderValue.isEnabled = true
        binding.dobData.isEnabled = true
        binding.pincodeValue.isEnabled = true
        binding.searchIcon.isEnabled = true
        binding.locationNameValue.isEnabled = true
        binding.districtNameValue.isEnabled = true
        binding.stateNameValue.isEnabled = true
        binding.countryValue.isEnabled = true
        binding.line1Value.isEnabled = true
        binding.line2Value.isEnabled = true
    }

    private fun disableUI() {
        binding.saveButton.isEnabled = false
        binding.backButton.isEnabled = false
        binding.firstNameValue.isEnabled = false
        binding.middleNameValue.isEnabled = false
        binding.lastNameValue.isEnabled = false
        binding.emailValue.isEnabled = false
        binding.mobileValue.isEnabled = false
        binding.genderValue.isEnabled = false
        binding.dobData.isEnabled = false
        binding.pincodeValue.isEnabled = false
        binding.searchIcon.isEnabled = false
        binding.locationNameValue.isEnabled = false
        binding.districtNameValue.isEnabled = false
        binding.stateNameValue.isEnabled = false
        binding.countryValue.isEnabled = false
        binding.line1Value.isEnabled = false
        binding.line2Value.isEnabled = false
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
        if (userId != -1) {
            viewModel.fetchProfileDetails(userId)
        } else {
            Toast.makeText(context, "User ID not available", Toast.LENGTH_SHORT).show()
        }

        viewModel.profileDetails.observe(viewLifecycleOwner) { response ->
            context?.let {
                binding.apply {
                    if (response != null) {
                        firstNameValue.setText(response.response_message.first_name)
                    }
                    if (response != null) {
                        middleNameValue.setText(response.response_message.middle_name ?: "")
                    }
                    if (response != null) {
                        lastNameValue.setText(response.response_message.last_name)
                    }
                    if (response != null) {
                        emailValue.setText(response.response_message.user_email)
                    }
                    if (response != null) {
                        mobileValue.setText(response.response_message.user_phone_number)
                    }
                    if (response != null) {
                        genderValue.setText(response.response_message.gender ?: "")
                    }
                    if (response != null) {
                        dobData.setText(response.response_message.date_of_birth)
                    }
                    if (response != null) {
                        pincodeValue.setText(response.response_message.pin_code?.toString() ?: "")
                    }
                    if (response != null) {
                        locationNameValue.setText(response.response_message.location_name ?: "")
                    }
                    if (response != null) {
                        districtNameValue.setText(response.response_message.district_name ?: "")
                    }
                    if (response != null) {
                        stateNameValue.setText(response.response_message.state_name ?: "")
                    }
                    if (response != null) {
                        countryValue.setText(response.response_message.country_name ?: "")
                    }
                    if (response != null) {
                        line1Value.setText(response.response_message.user_address_line_1 ?: "")
                    }
                    if (response != null) {
                        line2Value.setText(response.response_message.user_address_line_2 ?: "")
                    }
                }
            }
        }

        binding.backButton.setOnClickListener {
            val homeScreen = parentFragment as? HomeScreen
            homeScreen?.navigateProfileDetailsFragment()
        }

        binding.saveButton.setOnClickListener {
            if (!isInternetAvailable()) {  // Check internet *before* proceeding
                showNoInternetToast() // Or a more specific message
                return@setOnClickListener // Stop execution if no internet
            }
            if (!validateFields()) { // New validation function
                return@setOnClickListener
            }
            val userEmail = binding.emailValue.text.toString()
            val dateOfBirth = binding.dobData.text.toString()
            val pinCodeString = binding.pincodeValue.text.toString()

            val emailToSend = if (userEmail.isBlank()) {
                null
            } else if (!isValidEmail(userEmail)) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                userEmail
            }

            val formattedDateOfBirth = if (dateOfBirth.isNotBlank()) {
                formatDateOfBirth(dateOfBirth)
            } else {
                null
            }
            if (formattedDateOfBirth == null && dateOfBirth.isNotBlank()) {
                Toast.makeText(requireContext(), "Please enter a valid date of birth (dd-MM-yyyy)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pinCodeString.isNotBlank() && pinCodeString.toIntOrNull() == null) {
                Toast.makeText(requireContext(), "Please enter a valid pin code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val profileRequest = EditProfileRequest(
                user_id = userId,
                first_name = binding.firstNameValue.text.toString(),
                middle_name = binding.middleNameValue.text.toString(),
                last_name = binding.lastNameValue.text.toString(),
                gender = binding.genderValue.text.toString().takeIf { it.isNotBlank() },
                user_email = emailToSend.toString(),
                user_phone_number = binding.mobileValue.text.toString(),
                user_type = null.toString(),
                date_of_birth = formattedDateOfBirth ?: dateOfBirth,
                user_address_line_1 = binding.line1Value.text.toString(),
                user_address_line_2 = binding.line2Value.text.toString(),
                pin_code = pinCodeString,
                location_name = binding.locationNameValue.text.toString().takeIf { it.isNotBlank() },
                district_name = binding.districtNameValue.text.toString().takeIf { it.isNotBlank() },
                state_name = binding.stateNameValue.text.toString().takeIf { it.isNotBlank() },
                country_name = binding.countryValue.text.toString().takeIf { it.isNotBlank() }
            )

            viewModel.updateProfileDetails(userId.toString(), profileRequest)
        }

        viewModel.editProfileResult.observe(viewLifecycleOwner) { response ->
            if (response.response == "success") {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                val homeScreen = parentFragment as? HomeScreen
                homeScreen?.navigateProfileDetailsFragment()
            } else {
                Toast.makeText(requireContext(), "Failed to update profile: ${response.response_message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun validateFields(): Boolean {
        if (binding.firstNameValue.text.toString().isBlank()) {
            showToast("First name cannot be empty")
            return false
        }

        if (binding.lastNameValue.text.toString().isBlank()) {
            showToast("Last name cannot be empty")
            return false
        }

        if (binding.genderValue.text.toString().isBlank()) {
            showToast("Gender cannot be empty")
            return false
        }
        if (binding.emailValue.text.toString().isBlank()) {
            showToast("Email cannot be empty")
            return false
        }

        if (binding.dobData.text.toString().isBlank()) {
            showToast("Date of birth cannot be empty")
            return false
        }

        if (binding.pincodeValue.text.toString().isBlank()) {
            showToast("Pincode cannot be empty")
            return false
        }

        if (binding.locationNameValue.text.toString().isBlank()) {
            showToast("Location name cannot be empty")
            return false
        }
        if (binding.line1Value.text.toString().isBlank()) {
            showToast("Address line 1 cannot be empty")
            return false
        }
        if (binding.line2Value.text.toString().isBlank()) {
            showToast("Address line 2 cannot be empty")
            return false
        }
        if (binding.districtNameValue.text.toString().isBlank()) {
            showToast("District cannot be empty")
            return false
        }
        if (binding.stateNameValue.text.toString().isBlank()) {
            showToast("State cannot be empty")
            return false
        }
        if (binding.countryValue.text.toString().isBlank()) {
            showToast("Country cannot be empty")
            return false
        }


        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showNoInternetToast() {
        Toast.makeText(requireContext(), "No internet connection. Please try again later.", Toast.LENGTH_SHORT).show()
    }


    private fun disableLocationFields() {
        binding.districtNameValue.isEnabled = false
        binding.stateNameValue.isEnabled = false
        binding.countryValue.isEnabled = false

        binding.districtNameValue.setAdapter(null)
        binding.districtNameValue.text.clear()

        binding.stateNameValue.setAdapter(null)
        binding.stateNameValue.text.clear()

        binding.countryValue.setAdapter(null)
        binding.countryValue.text.clear()

        binding.locationNameValue.setAdapter(null)
        binding.locationNameValue.text.clear()
    }

    private fun enableLocationFields(locationList: List<LocationDetail>) {
        binding.districtNameValue.isEnabled = true
        binding.stateNameValue.isEnabled = true
        binding.countryValue.isEnabled = true
        binding.locationNameValue.isEnabled = true

        populateDropdowns(locationList)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        view?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            it.clearFocus()
        }
    }

    private fun showDatePicker(dobField: AutoCompleteTextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        calendar.set(year - 18, month, day)
        val maxDate = calendar.timeInMillis

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                dobField.setText(selectedDate)
            },
            year - 18,
            month,
            day
        )
        datePickerDialog.datePicker.maxDate = maxDate
        datePickerDialog.show()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun formatDateOfBirth(dateOfBirth: String): String? {
        return try {
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = outputFormat.parse(dateOfBirth)
            outputFormat.format(date)
        } catch (e: Exception) {
            null
        }
    }

    private fun populateDropdowns(locationList: List<LocationDetail>) {
        val locations = locationList.map { it.location }
        val districts = locationList.map { it.district }.distinct()
        val states = locationList.map { it.state }.distinct()
        val countries = locationList.map { it.country }.distinct()

        binding.locationNameValue.apply {
            setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, locations))
        }

        binding.districtNameValue.apply {
            setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, districts))
        }

        binding.stateNameValue.apply {
            setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, states))
        }

        binding.countryValue.apply {
            setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, countries))
        }
    }

    override fun onResume() {
        super.onResume()
        checkInternetAndSetup()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isViewCreated = false
    }
}