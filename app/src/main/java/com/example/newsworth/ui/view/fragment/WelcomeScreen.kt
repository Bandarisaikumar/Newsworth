package com.example.newsworth.ui.view.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.newsworth.R
import com.example.newsworth.databinding.FragmentWelcomeScreenBinding
import com.example.newsworth.ui.adapter.BannerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class WelcomeScreen : Fragment() {

    private lateinit var binding: FragmentWelcomeScreenBinding
    private lateinit var bannerViewPager: ViewPager2
    private lateinit var bannerIndicator: TabLayout
    private lateinit var bannerAdapter: BannerAdapter
    private val bannerImages = listOf(
        R.drawable.image_one,
        R.drawable.image_two,
        R.drawable.image_three,
        R.drawable.image_five,
        R.drawable.image_six
    )
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            val currentItem = bannerViewPager.currentItem
            // Move to the next item, but jump directly to the first item after the last one without scrolling back
            val nextItem = if (currentItem == bannerImages.size - 1) {
                0  // Jump to the first image after the last one without animation
            } else {
                currentItem + 1  // Otherwise, go to the next item
            }
            // To avoid backtracking, disable smooth scroll when jumping to the first item
            bannerViewPager.setCurrentItem(nextItem, currentItem != bannerImages.size - 1)
            handler.postDelayed(this, 3000) // Auto-scroll every 3 seconds
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomeScreenBinding.inflate(inflater, container, false)

        binding.loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeScreen_to_loginScreen)
        }
        binding.signUpButton.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeScreen_to_registrationScreen)
        }

        bannerViewPager = binding.bannerViewPager
        bannerIndicator = binding.bannerIndicator
        bannerAdapter = BannerAdapter(bannerImages)
        bannerViewPager.adapter = bannerAdapter

        // Prevent auto-scrolling wrapping behavior by setting the page limit to 1
        bannerViewPager.offscreenPageLimit = 1

        // Set up the TabLayout with the ViewPager2
        if (bannerIndicator.tabCount == 0) { // Prevent reinitializing tabs
            TabLayoutMediator(bannerIndicator, bannerViewPager) { _, _ -> }.attach()
        }

        // Start auto-scrolling
        handler.postDelayed(runnable, 3000)
        // Handle the back gesture
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // If this is the first screen (welcome screen), close the app
            if (findNavController().currentDestination?.id == R.id.welcomeScreen) {
                requireActivity().finish() // Close the app
            } else {
                findNavController().navigateUp() // Navigate up if not on the welcome screen
            }
        }
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Stop scrolling when fragment is destroyed
    }
}
