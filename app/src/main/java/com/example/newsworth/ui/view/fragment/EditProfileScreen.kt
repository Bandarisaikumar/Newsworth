package com.example.newsworth.ui.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Filter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
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

    private lateinit var viewModel: ProfileManagementViewmodel // Shared ViewModel
    private lateinit var binding: FragmentEditProfileScreenBinding // View binding for the layout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditProfileScreenBinding.inflate(inflater, container, false)

        val profileRepository = ProfileManagementRepository(RetrofitClient.getApiService(requireContext()))
        val profileFactory = ProfileManagementViewmodelFactory(profileRepository)
        viewModel = ViewModelProvider(this, profileFactory).get(ProfileManagementViewmodel::class.java)

//        // Setup listener for pincode input
//        binding.pincodeValue.setOnEditorActionListener { _, actionId, _ ->
//            if (actionId == EditorInfo.IME_ACTION_DONE) {
//                val pincode = binding.pincodeValue.text.toString()
//                if (pincode.isNotEmpty()) {
//                    viewModel.fetchLocationDetails(pincode.toInt())
//                } else {
//                    Toast.makeText(requireContext(), "Enter a valid pincode", Toast.LENGTH_SHORT).show()
//                }
//                true
//            } else {
//                false
//            }
//        }
        // Automatically call API when user enters pincode
        binding.pincodeValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No action needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.locationNameValue.text.clear()
            }

            override fun afterTextChanged(s: Editable?) {
                val pincode = s.toString()
                if (pincode.length == 6) { // Assuming pincode is 6 digits long
                    viewModel.fetchLocationDetails(pincode.toInt())
                }
            }
        })

        // Reference to TextInputLayout and AutoCompleteTextView
        val genderDropdown: AutoCompleteTextView = binding.genderValue

// List of gender options
        val genderOptions = listOf("Male", "Female", "Other")

// Set up ArrayAdapter
        // Set up ArrayAdapter with a custom filter to prevent filtering
        val genderAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            genderOptions
        ) {
            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        // Always return the full list of options
                        val results = FilterResults()
                        results.values = genderOptions
                        results.count = genderOptions.size
                        return results
                    }

                    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                        // Notify the adapter with the unfiltered data
                        notifyDataSetChanged()
                    }
                }
            }
        }

// Attach adapter to AutoCompleteTextView
        genderDropdown.setAdapter(genderAdapter)

// Ensure the dropdown is shown when the user clicks on the field
        genderDropdown.setOnClickListener {
            genderDropdown.dismissDropDown()
            genderDropdown.setAdapter(genderAdapter)
            genderDropdown.showDropDown()
        }


        // Optional: Handle selection
        genderDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedGender = genderOptions[position]
            // Perform actions based on selection
            println("Selected Gender: $selectedGender")
        }

        // Date of Birth Calendar Dialog
        val dobField: AutoCompleteTextView= binding.dobData
        dobField.setOnClickListener {
            showDatePicker(dobField)
        }

        // Observe location details from the ViewModel
        viewModel.locationDetails.observe(viewLifecycleOwner) { locationList ->
            populateDropdowns(locationList)
        }

        // Observe error messages from the ViewModel
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
        if (userId != -1) {
            viewModel.fetchProfileDetails(userId) // Fetch data when the fragment is created
        } else {
            Toast.makeText(context, "User ID not available", Toast.LENGTH_SHORT).show()
        }

        viewModel.profileDetails.observe(viewLifecycleOwner) {  response ->
            context?.let {
                Toast.makeText(context, response.response, Toast.LENGTH_SHORT).show()

                // Bind the response data to the UI elements
                binding.apply {
                    firstNameValue.setText(response.response_message.first_name)
                    middleNameValue.setText(response.response_message.middle_name ?: "")
                    lastNameValue.setText(response.response_message.last_name)
                    emailValue.setText(response.response_message.user_email)
                    mobileValue.setText(response.response_message.user_phone_number)
                    genderValue.setText(response.response_message.gender ?: "")
                    dobData.setText(response.response_message.date_of_birth)
                    pincodeValue.setText(response.response_message.pin_code?.toString() ?: "")
                    locationNameValue.setText(response.response_message.location_name ?: "")
                    districtNameValue.setText(response.response_message.district_name ?: "")
                    stateNameValue.setText(response.response_message.state_name ?: "")
                    countryValue.setText(response.response_message.country_name ?: "")
                    line1Value.setText(response.response_message.user_address_line_1 ?: "")
                    line2Value.setText(response.response_message.user_address_line_2 ?: "")

                }
            }
        }
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_editProfileScreen_to_profileDetailsScreen)

        }


        // Set up Save Button click listener
        binding.saveButton.setOnClickListener {
            // Collect data from input fields
            val userEmail = binding.emailValue.text.toString()
            val dateOfBirth = binding.dobData.text.toString()
            val pinCodeString = binding.pincodeValue.text.toString()

            // Validate email only if entered
            if (userEmail.isNotBlank() && !isValidEmail(userEmail)) {
                Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate date of birth format (only if entered)
            val formattedDateOfBirth = if (dateOfBirth.isNotBlank()) {
                formatDateOfBirth(dateOfBirth)
            } else {
                null // Skip validation if DOB is not entered
            }
            if (formattedDateOfBirth == null && dateOfBirth.isNotBlank()) {
                Toast.makeText(requireContext(), "Please enter a valid date of birth (dd-MM-yyyy)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // Validate pin code (only if entered)
            if (pinCodeString.isNotBlank() && pinCodeString.toIntOrNull() == null) {
                Toast.makeText(requireContext(), "Please enter a valid pin code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Prepare the profile update request
            val profileRequest = EditProfileRequest(
                user_id = userId,
                first_name = binding.firstNameValue.text.toString(),
                middle_name = binding.middleNameValue.text.toString(),
                last_name = binding.lastNameValue.text.toString(),
                gender = binding.genderValue.text.toString().takeIf { it.isNotBlank() },
                user_email = userEmail,
                user_phone_number = binding.mobileValue.text.toString(),
                user_type = null.toString(),
                date_of_birth = formattedDateOfBirth ?: dateOfBirth, // Use formatted DOB or original if empty
                user_address_line_1 = binding.line1Value.text.toString(),
                user_address_line_2 = binding.line2Value.text.toString(),
                pin_code = pinCodeString,
                location_name = binding.locationNameValue.text.toString().takeIf { it.isNotBlank() },
                district_name = binding.districtNameValue.text.toString().takeIf { it.isNotBlank() },
                state_name = binding.stateNameValue.text.toString().takeIf { it.isNotBlank() },
                country_name = binding.countryValue.text.toString().takeIf { it.isNotBlank() }
            )

            // Call the ViewModel method
            viewModel.updateProfileDetails(userId.toString(), profileRequest)
        }

        // Observe API response
        viewModel.editProfileResult.observe(viewLifecycleOwner) { response ->
            if (response.response == "success") {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_editProfileScreen_to_profileDetailsScreen)

            } else {
                Toast.makeText(requireContext(), "Failed to update profile: ${response.response_message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showDatePicker(dobField: AutoCompleteTextView) {
        // Get the current date
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Set the maximum date to 18 years ago
        calendar.set(year - 18, month, day)
        val maxDate = calendar.timeInMillis

        // Show DatePickerDialog
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

    // Email validation function
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Function to format the date of birth (dd-MM-yyyy to yyyy-MM-dd)
    private fun formatDateOfBirth(dateOfBirth: String): String? {
        return try {
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = outputFormat.parse(dateOfBirth)
            outputFormat.format(date) // Return the formatted date
        } catch (e: Exception) {
            null // Return null if the date format is invalid
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
}
