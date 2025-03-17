package com.example.newsworth.ui.view.fragment

import android.app.AlertDialog
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.databinding.FragmentProfileDetailsScreenBinding
import com.example.newsworth.repository.ProfileManagementRepository
import com.example.newsworth.ui.viewmodel.ProfileManagementViewmodel
import com.example.newsworth.ui.viewmodel.ProfileManagementViewmodelFactory
import com.example.newsworth.utils.SharedPrefModule

class ProfileDetailsScreen : Fragment() {

    private var binding: FragmentProfileDetailsScreenBinding? = null
    private lateinit var viewModel: ProfileManagementViewmodel
    private var isInternetAvailable: Boolean = true
    private var isViewCreated: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val homeScreen = parentFragment as? HomeScreen
                homeScreen?.showAccountScreen()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileDetailsScreenBinding.inflate(inflater, container, false)
        isViewCreated = true

        val profileRepository = ProfileManagementRepository(RetrofitClient.getApiService(requireContext()))
        val profileFactory = ProfileManagementViewmodelFactory(profileRepository)
        viewModel = ViewModelProvider(this, profileFactory)[ProfileManagementViewmodel::class.java]

        checkInternetAndSetup()

        binding!!.backButton.setOnClickListener {
            val homeScreen = parentFragment as? HomeScreen
            homeScreen?.showAccountScreen()
        }

        return binding!!.root
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
        binding?.backButton?.isEnabled = true
        binding?.editButton?.isEnabled = true
    }

    private fun disableUI() {
        binding?.backButton?.isEnabled = false
        binding?.editButton?.isEnabled = false
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
                binding?.apply {
                    val fullName = buildString {
                        if (response != null) {
                            append(response.response_message.first_name)
                        }
                        if (response != null) {
                            if (!response.response_message.middle_name.isNullOrEmpty()) {
                                append(" ${response.response_message.middle_name}")
                            }
                        }
                        if (response != null) {
                            if (response.response_message.last_name.isNotEmpty()) {
                                append(" ${response.response_message.last_name}")
                            }
                        }
                    }
                    nameValue.text = fullName.ifEmpty { " " }
                    if (response != null) {
                        emailValue.text = response.response_message.user_email
                    }
                    if (response != null) {
                        phoneValue.text = response.response_message.user_phone_number
                    }
                    if (response != null) {
                        genderValue.text = response.response_message.gender
                    }
                    if (response != null) {
                        dobValue.text = response.response_message.date_of_birth
                    }
                    if (response != null) {
                        address1Value.text = response.response_message.user_address_line_1
                    }
                    if (response != null) {
                        address2Value.text = response.response_message.user_address_line_2
                    }
                    if (response != null) {
                        pincodeValue.text = response.response_message.pin_code.toString()
                    }
                    if (response != null) {
                        districtValue.text = response.response_message.district_name
                    }
                    if (response != null) {
                        locationValue.text = response.response_message.location_name
                    }
                    if (response != null) {
                        stateValue.text = response.response_message.state_name
                    }
                    if (response != null) {
                        contryValue.text = response.response_message.country_name
                    }
                }
            }
        }

        binding?.editButton?.setOnClickListener {
            isInternetAvailable = isInternetAvailable()
            if (isInternetAvailable) {
                val profileDetails = viewModel.profileDetails.value?.response_message
                if (profileDetails != null) {
                    viewModel.setEditProfileData(profileDetails)
                    val homeScreen = parentFragment as? HomeScreen
                    homeScreen?.navigateProfileEditFragment()
                }
            }else{
                showNoInternetToast()
            }
        }
    }
    private fun showNoInternetToast() {
        Toast.makeText(requireContext(), "No internet connection.please turn on internet", Toast.LENGTH_SHORT).show()
    }



    override fun onResume() {
        super.onResume()
        checkInternetAndSetup()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        isViewCreated = false
    }
}