package com.example.newsworth.utils

import kotlinx.coroutines.delay

object NetworkUtils {

    // Retry IO function
    suspend fun <T> retryIO(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 5000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()  // Attempt the operation
            } catch (e: Exception) {
                e.printStackTrace()  // Log the exception if needed
            }
            delay(currentDelay)  // Wait before retrying
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // Last attempt if all retries fail
    }
}
