package com.example.newsworth.data.model

class RegistrationModels {

    // Registration
    data class RegistrationRequest(
        val platform: String = "Web",
        val first_name: String,
        val middle_name: String? = null,
        val last_name: String,
        val dob: String,
        val gender: String,
        val country: String,
        val user_type: String,
        val user_email: String,
        val user_phone_number: String,
        val password: String,
        val confirm_password: String
    )

    data class RegistrationResponse(
        val response: String,
        val response_message: String
    )

    // Send OTP
    data class SendOtpRequest(
        val first_name: String,
        val mobile: String,
        val email: String? = null
    )

    data class SendOtpResponse(
        val response: String,
        val response_message: String,

        val data : String
    )

    // Signup Verification
    data class SignupVerificationRequest(
        val email: String,
        val mobile: String,
        val email_otp: String,
        val mobile_otp: String
    )

    data class SignupVerificationResponse(
        val response: String,
        val response_message: String,
        val mobile_response: VerificationDetail,
        val email_response: VerificationDetail

    )

    data class VerificationDetail(
        val response: String,
        val response_message: String,

    )


}