package com.example.newsworth.data.api

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://newsworth.com:8014/"

    private fun createOkHttpClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(context))
            .readTimeout(20, TimeUnit.MINUTES)
            .connectTimeout(20, TimeUnit.MINUTES)
            .build()
    }

    private fun getInstance(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun getApiService(context: Context): ApiService {
        val retrofit = getInstance(context)
        return retrofit.create(ApiService::class.java)
    }
}
