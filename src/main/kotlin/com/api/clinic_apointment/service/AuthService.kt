package com.api.clinic_apointment.service

import com.api.clinic_apointment.config.JwtUtil
import com.api.clinic_apointment.dto.LoginRequest
import com.api.clinic_apointment.dto.LoginResponse
import com.api.clinic_apointment.repository.UserRepository
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {
    companion object {
        private const val DUMMY_HASH = "\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
    }

    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)

        if (user == null) {
            passwordEncoder.matches(request.password, DUMMY_HASH)
            throw BadCredentialsException("Invalid email or password")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash))
            throw BadCredentialsException("Invalid email or password")

        val token = jwtUtil.generateToken(user.id, user.role)
        return LoginResponse(token)
    }
}
