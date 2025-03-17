package com.example.newsworth.ui.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.example.newsworth.R
import com.example.newsworth.databinding.FragmentSigninSignupScreenBinding

class SigninSignupScreen : Fragment() {

    private lateinit var binding: FragmentSigninSignupScreenBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSigninSignupScreenBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_signinSignupScreen_to_welcomeScreen)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signup.setOnClickListener {
            findNavController().navigate(R.id.action_signinSignupScreen_to_registrationScreen)
        }

        binding.signin.setOnClickListener {
            findNavController().navigate(R.id.action_signinSignupScreen_to_loginScreen)
        }
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_signinSignupScreen_to_welcomeScreen)

        }
    }
}