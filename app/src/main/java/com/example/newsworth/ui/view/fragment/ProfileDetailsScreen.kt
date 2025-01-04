package com.example.newsworth.ui.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.databinding.FragmentProfileDetailsScreenBinding
import com.example.newsworth.repository.ProfileManagementRepository
import com.example.newsworth.ui.viewmodel.ProfileManagementViewmodel
import com.example.newsworth.ui.viewmodel.ProfileManagementViewmodelFactory
import com.example.newsworth.utils.SharedPrefModule

class ProfileDetailsScreen : Fragment() {

    private var binding: FragmentProfileDetailsScreenBinding? = null
    private lateinit var viewModel: ProfileManagementViewmodel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileDetailsScreenBinding.inflate(inflater, container, false)

        val profileRepository = ProfileManagementRepository(RetrofitClient.getApiService(requireContext()))
        val profileFactory = ProfileManagementViewmodelFactory(profileRepository)
        viewModel = ViewModelProvider(this, profileFactory).get(ProfileManagementViewmodel::class.java)

        binding!!.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileDetailsScreen_to_userProfileFragment)
        }


        return binding!!.root
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
            // Check if context is non-null before using it
            context?.let {
                Toast.makeText(context, response.response, Toast.LENGTH_SHORT).show()

                // Bind the response data to the UI elements
                binding?.apply {
                    // Set the profile details
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
                    // Set the full name to the nameValue TextView
                    nameValue.text = fullName.ifEmpty { "N/A" }
                    emailValue.text = response.response_message.user_email
                    phoneValue.text = response.response_message.user_phone_number
                    genderValue.text = response.response_message.gender
                    dobValue.text = response.response_message.date_of_birth
                    // Concatenate total address
                    val address = buildString {
                        append(response.response_message.location_name ?: "")
                        if (!response.response_message.district_name.isNullOrEmpty()) {
                            append(" ${response.response_message.district_name}")
                        }
                        if (!response.response_message.state_name.isNullOrEmpty()) {
                            append(" ${response.response_message.state_name}")
                        }
                        if (response.response_message.pin_code != null) { // Check if pin_code is not null
                            append(" ${response.response_message.pin_code}")
                        }
                        if (!response.response_message.country_name.isNullOrEmpty()) {
                            append(" ${response.response_message.country_name}")
                        }
                    }
                    address1Value.text = response.response_message.user_address_line_1
                    address2Value.text = response.response_message.user_address_line_2
                    pincodeValue.text = response.response_message.pin_code.toString()
                    districtValue.text = response.response_message.district_name
                    locationValue.text = response.response_message.location_name
                    stateValue.text = response.response_message.state_name
                    contryValue.text = response.response_message.country_name

                }
            }
        }
        binding?.editButton?.setOnClickListener {
            val profileDetails = viewModel.profileDetails.value?.response_message
            if (profileDetails != null) {
                viewModel.setEditProfileData(profileDetails) // Pass the ProfileDetails object
                findNavController().navigate(R.id.action_profileDetailsScreen_to_editProfileScreen)
            }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null // Avoid memory leaks
    }
}
