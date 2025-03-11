package com.example.newsworth.ui.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.newsworth.R
import com.example.newsworth.data.api.RetrofitClient
import com.example.newsworth.databinding.FragmentHomeScreenBinding
import com.example.newsworth.repository.UserManagementRepository
import com.example.newsworth.ui.viewmodel.UserManagementViewModel
import com.example.newsworth.ui.viewmodel.UserManagementViewModelFactory
import com.example.newsworth.utils.SharedPrefModule
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeScreen : Fragment() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private var currentFragmentTag: String? = null
    private var mediaUploadBundle: Bundle? = null // Store the bundle

    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserManagementViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mediaUploadBundle = savedInstanceState.getBundle("mediaUploadBundle") // Restore the bundle
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        val view = binding.root

        val userRepository = UserManagementRepository(RetrofitClient.getApiService(requireContext()))
        val userFactory = UserManagementViewModelFactory(userRepository)
        userViewModel = ViewModelProvider(this, userFactory)[UserManagementViewModel::class.java]

        binding.logoutButton.setOnClickListener {
            performLogout()
        }

        observeViewModels()


        bottomNavigationView = view.findViewById(R.id.bottom_nav_view)

        if (savedInstanceState == null) {
            showFragment("home") // Show initial fragment
        } else {
            currentFragmentTag = savedInstanceState.getString("currentFragmentTag")
            if (currentFragmentTag != null) {
                showFragment(currentFragmentTag!!)
            } else {
                showFragment("home")
            }
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            val newTag = when (item.itemId) {
                R.id.navigation_myfiles -> "user_screen"
                R.id.navigation_account -> "account"
                R.id.navigation_chat -> "chat"
                R.id.navigation_home -> "home"
                else -> return@setOnItemSelectedListener false
            }

            showFragment(newTag)
            return@setOnItemSelectedListener true
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentFragmentTag", currentFragmentTag)
        outState.putBundle("mediaUploadBundle", mediaUploadBundle) // Save the bundle
    }
    private fun performLogout() {
        val userId = SharedPrefModule.provideTokenManager(requireContext()).userId?.toInt() ?: -1
        if (userId != -1) {
            userViewModel.logoutUser(userId)
        } else {
            Toast.makeText(requireContext(), "User ID not found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModels() {
        userViewModel.logoutResponse.observe(viewLifecycleOwner) { response ->
            if (response != null && response.response == "success") {
                Toast.makeText(context, response.response_message, Toast.LENGTH_LONG).show()
                SharedPrefModule.provideTokenManager(requireContext()).clearTokens()
                findNavController().navigate(R.id.action_homeScreen_to_welcomeScreen)
            } else {
                Toast.makeText(context, "Logout failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }

        userViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                userViewModel.clearErrorMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showFragment(tag: String) {
        currentFragmentTag = tag
        val transaction = childFragmentManager.beginTransaction()

        val fragment = when (tag) {
            "user_screen" -> UserScreen()
            "account" -> UserProfile()
            "chat" -> ChatContent()
            "home" -> HomeContent()
            "images" -> ImagesSection()
            "videos" -> VideoSection()
            "audios" -> AudiosSection()
            "details" -> ProfileDetailsScreen()
            "edit" -> EditProfileScreen()
            "about" -> AboutAppScreen()
            "media_upload" -> {
                val mediaUploadFragment = MediaUploadFragment()
                if (mediaUploadBundle != null) { // Pass the bundle if it exists
                    mediaUploadFragment.arguments = mediaUploadBundle
                    mediaUploadBundle = null // Clear the bundle after use
                }
                mediaUploadFragment
            }
            else -> return // Handle unknown tags
        }

        transaction.replace(R.id.fragment_container, fragment, tag)
        transaction.commit()
    }

    // --- Navigation Functions (Use showFragment with tag) ---
    fun navigateToImagesFragment() {
        showFragment("images")
    }

    fun navigateToVideosFragment() {
        showFragment("videos")
    }

    fun navigateToAudiosFragment() {
        showFragment("audios")
    }

    fun navigateProfileDetailsFragment() {
        showFragment("details")
    }
    fun navigateAboutAppFragment() {
        showFragment("about")
    }

    fun navigateProfileEditFragment() {
        showFragment("edit")
    }

    fun loadMediaUploadFragment(bundle: Bundle) {
        mediaUploadBundle = bundle // Store the bundle
        showFragment("media_upload")
    }
    // In HomeScreen.kt
    fun showHomeContentTabAndSettingsDialog() {
        showHomeContentTab() // Switch to the HomeContent tab

        // Use a postDelayed to give HomeContent time to load
        view?.postDelayed({
            val homeContentFragment = childFragmentManager.fragments.find { it is HomeContent } as? HomeContent
            homeContentFragment?.showPositionSelectionDialog()
        }, 200) // Adjust the delay as needed (e.g., 200 milliseconds)
    }

    fun showAccountScreen() {
        showFragment("account")
    }

    fun showUserScreen() {
        showFragment("user_screen")
    }

    fun showUserScreenForVideos() {
        showFragment("user_screen")
    }

    fun showUserScreenForAudios() {
        showFragment("user_screen")
    }

    fun showMyFilesTab() {
        bottomNavigationView.selectedItemId = R.id.navigation_myfiles
    }

    fun showHomeContentTab() {
        bottomNavigationView.selectedItemId = R.id.navigation_home
    }

    fun showAccountsTab() {
        bottomNavigationView.selectedItemId = R.id.navigation_account
    }
}