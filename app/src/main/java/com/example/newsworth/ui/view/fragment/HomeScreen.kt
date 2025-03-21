package com.example.newsworth.ui.view.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
    private var mediaUploadBundle: Bundle? = null

    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var userViewModel: UserManagementViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mediaUploadBundle = savedInstanceState.getBundle("mediaUploadBundle")
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
            showFragment("home")
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
        view.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                insets.systemWindowInsetBottom
            )
            insets
        }

        view.requestApplyInsets()

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentFragmentTag", currentFragmentTag)
        outState.putBundle("mediaUploadBundle", mediaUploadBundle)
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
            "helpandsupport" -> HelpAndSupport()
            "media_upload" -> {
                val mediaUploadFragment = MediaUploadFragment()
                if (mediaUploadBundle != null) {
                    mediaUploadFragment.arguments = mediaUploadBundle
                    mediaUploadBundle = null
                }
                mediaUploadFragment
            }
            else -> return
        }

        transaction.replace(R.id.fragment_container, fragment, tag)
        transaction.commit()
    }

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
    fun navigateHelpAndSupport(){
        showFragment("helpandsupport")
    }

    fun navigateProfileEditFragment() {
        showFragment("edit")
    }

    fun loadMediaUploadFragment(bundle: Bundle) {
        mediaUploadBundle = bundle
        showFragment("media_upload")
    }
    // In HomeScreen.kt
    fun showHomeContentTabAndSettingsDialog() {
        showHomeContentTab()

        view?.postDelayed({
            val homeContentFragment = childFragmentManager.fragments.find { it is HomeContent } as? HomeContent
            homeContentFragment?.showPositionSelectionDialog()
        }, 200)
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

}