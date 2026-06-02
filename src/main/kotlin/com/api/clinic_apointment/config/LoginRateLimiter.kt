package com.api.clinic_apointment.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class LoginRateLimiter {
    private val attempts: Cache<String, AttemptWindow> = Caffeine.newBuilder()
        .expireAfterWrite(WINDOW)
        .maximumSize(MAX_KEYS)
        .build()

    fun isBlocked(key: String): Boolean {
        val window = attempts.getIfPresent(key) ?: return false
        if (isExpired(window)) {
            attempts.invalidate(key)
            return false
        }
        return window.failures >= MAX_FAILURES
    }

    fun recordFailure(key: String) {
        val now = Instant.now()
        attempts.asMap().compute(key) { _, current ->
            if (current == null || Duration.between(current.firstFailureAt, now) > WINDOW) {
                AttemptWindow(1, now)
            } else {
                current.copy(failures = current.failures + 1)
            }
        }
    }

    fun reset(key: String) {
        attempts.invalidate(key)
    }

    fun resetAll() {
        attempts.invalidateAll()
    }

    internal fun attemptCountForTesting(): Long {
        attempts.cleanUp()
        return attempts.estimatedSize()
    }

    private fun isExpired(window: AttemptWindow): Boolean {
        return Duration.between(window.firstFailureAt, Instant.now()) > WINDOW
    }

    private data class AttemptWindow(val failures: Int, val firstFailureAt: Instant)

    companion object {
        private const val MAX_FAILURES = 5
        private const val MAX_KEYS = 10_000L
        private val WINDOW = Duration.ofMinutes(5)
    }
}
