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
import com.example.newsworth.data.model.BannerData
import com.example.newsworth.databinding.FragmentWelcomeScreenBinding
import com.example.newsworth.ui.adapter.BannerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class WelcomeScreen : Fragment() {

    private lateinit var binding: FragmentWelcomeScreenBinding
    private val bannerDataList = listOf(
        BannerData(
            R.drawable.image_one,
            "Unfiltered stories",
            "Real, unfiltered news-authentic, raw, and factual-keeping you informed with stories that matter, straight from the source."
        ),
        BannerData(
            R.drawable.image_two,
            "Certified Content",
            "Premium, certified news-verified, trustworthy, and ready for monetization, offering both credibility and revenue opportunities."
        ),
        BannerData(
            R.drawable.image_three,
            "NewsWorth Eye",
            "Capture content using the NewsWorth Eye mobile app, with cloud storage."
        ),
        BannerData(
            R.drawable.image_five,
            "Pricing & Negotiation",
            "Competitive pricing, flexible negotiation, and top-quality content at reasonable rates."
        ),
        BannerData(
            R.drawable.image_six,
            "Security",
            "Encrypted with advanced digital security, ensuring only the creator and buyer can access the content."
        )
    )
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentBannerData = bannerDataList[0] // Get data for the initial page
        binding.heading.text = currentBannerData.heading
        binding.matter.text = currentBannerData.matter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomeScreenBinding.inflate(inflater, container, false)
        bannerViewPager = binding.bannerViewPager
        bannerIndicator = binding.bannerIndicator
        bannerAdapter = BannerAdapter(bannerImages)
        bannerViewPager.adapter = bannerAdapter

        TabLayoutMediator(bannerIndicator, bannerViewPager) { tab, position ->
            tab.view.setBackgroundResource(0) // Remove default background
            val tabView =
                LayoutInflater.from(requireContext()).inflate(R.layout.indicator_tab_layout, null)
            tab.customView = tabView // Set the custom view
            val indicatorDot = tabView.findViewById<View>(R.id.indicator_dot)
            // No need to set margins here; padding in the layout handles spacing
        }.attach()
        // Start auto-scrolling
        handler.postDelayed(runnable, 3000)
        bannerViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentBannerData = bannerDataList[position]
                binding.heading.text = currentBannerData.heading
                binding.matter.text = currentBannerData.matter
            }
        })
        // Handle the back gesture
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // If this is the first screen (welcome screen), close the app
            if (findNavController().currentDestination?.id == R.id.welcomeScreen) {
                requireActivity().finish() // Close the app
            } else {
                findNavController().navigateUp() // Navigate up if not on the welcome screen
            }
        }
        binding.getStarted.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeScreen_to_signinSignupScreen)
        }
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable) // Stop scrolling when fragment is destroyed
    }
}
