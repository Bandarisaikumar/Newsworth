package com.example.newsworth.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newsworth.repository.ProfileManagementRepository

class ProfileManagementViewmodelFactory(
    private val profileRepository: ProfileManagementRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileManagementViewmodel::class.java)) {
            return ProfileManagementViewmodel(profileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
