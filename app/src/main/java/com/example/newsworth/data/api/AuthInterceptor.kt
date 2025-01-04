package com.example.newsworth.data.api


import android.content.Context
import android.util.Log
import com.example.newsworth.utils.SharedPrefModule
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val accessToken = SharedPrefModule.provideTokenManager(context).accessToken
        Log.d("token","$accessToken")

        // Add Authorization header if token exists
        val newRequest = if (!accessToken.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}

