package com.api.clinic_apointment.controller

import com.api.clinic_apointment.config.LoginRateLimiter
import com.api.clinic_apointment.dto.LoginRequest
import com.api.clinic_apointment.dto.LoginResponse
import com.api.clinic_apointment.service.AuthService
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val loginRateLimiter: LoginRateLimiter
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        servletRequest: HttpServletRequest
    ): ResponseEntity<LoginResponse> {
        val key = servletRequest.remoteAddr ?: "unknown"
        if (loginRateLimiter.isBlocked(key)) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts")
        }
        return try {
            val response = authService.login(request)
            loginRateLimiter.reset(key)
            ResponseEntity.ok(response)
        } catch (ex: BadCredentialsException) {
            loginRateLimiter.recordFailure(key)
            throw ex
        }
    }
}
