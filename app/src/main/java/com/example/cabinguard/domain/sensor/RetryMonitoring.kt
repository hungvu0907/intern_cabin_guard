package com.example.cabinguard.domain.sensor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** A failed read/write must not become an uncaught foreground-service exception. */
suspend fun retryMonitoring(attempt: suspend () -> Unit, onFailure: (Exception) -> Unit) {
    while (currentCoroutineContext().isActive) {
        try {
            attempt()
            return
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            onFailure(error)
            delay(5_000)
        }
    }
}
