package com.api.clinic_apointment.service

import com.api.clinic_apointment.config.JwtUtil
import com.api.clinic_apointment.dto.LoginRequest
import com.api.clinic_apointment.dto.LoginResponse
import com.api.clinic_apointment.entity.User
import com.api.clinic_apointment.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtUtil: JwtUtil
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)
        jwtUtil = mock(JwtUtil::class.java)
        authService = AuthService(userRepository, passwordEncoder, jwtUtil)
    }

    @Test
    fun `login returns token when credentials are valid`() {
        val request = LoginRequest(email = "user@clinic.com", password = "correctPassword")
        val user = User(
            id = 1L,
            email = "user@clinic.com",
            passwordHash = "encodedHash",
            role = "RECEPTIONIST"
        )
        val expectedToken = "jwt-token-xyz"

        `when`(userRepository.findByEmail("user@clinic.com")).thenReturn(user)
        `when`(passwordEncoder.matches("correctPassword", "encodedHash")).thenReturn(true)
        `when`(jwtUtil.generateToken(1L, "RECEPTIONIST")).thenReturn(expectedToken)

        val response = authService.login(request)

        assertNotNull(response)
        assertEquals(expectedToken, response.token)
        verify(userRepository, times(1)).findByEmail("user@clinic.com")
        verify(passwordEncoder, times(1)).matches("correctPassword", "encodedHash")
        verify(jwtUtil, times(1)).generateToken(1L, "RECEPTIONIST")
    }

    @Test
    fun `login throws BadCredentialsException when user not found`() {
        val request = LoginRequest(email = "unknown@clinic.com", password = "anyPassword")

        `when`(userRepository.findByEmail("unknown@clinic.com")).thenReturn(null)

        assertThrows(BadCredentialsException::class.java) {
            authService.login(request)
        }
    }

    @Test
    fun `login throws BadCredentialsException when password is wrong`() {
        val request = LoginRequest(email = "user@clinic.com", password = "wrongPassword")
        val user = User(
            id = 1L,
            email = "user@clinic.com",
            passwordHash = "encodedHash",
            role = "RECEPTIONIST"
        )

        `when`(userRepository.findByEmail("user@clinic.com")).thenReturn(user)
        `when`(passwordEncoder.matches("wrongPassword", "encodedHash")).thenReturn(false)

        assertThrows(BadCredentialsException::class.java) {
            authService.login(request)
        }
    }

    @Test
    fun `login does not call jwtUtil generateToken when user not found`() {
        val request = LoginRequest(email = "unknown@clinic.com", password = "anyPassword")

        `when`(userRepository.findByEmail("unknown@clinic.com")).thenReturn(null)

        try {
            authService.login(request)
        } catch (_: BadCredentialsException) {
        }

        verify(jwtUtil, never()).generateToken(
            org.mockito.Mockito.anyLong(),
            org.mockito.Mockito.anyString()
        )
    }

    @Test
    fun `login calls passwordEncoder matches with dummy hash when user not found`() {
        val request = LoginRequest(email = "unknown@clinic.com", password = "anyPassword")

        `when`(userRepository.findByEmail("unknown@clinic.com")).thenReturn(null)

        try {
            authService.login(request)
        } catch (_: BadCredentialsException) {
        }

        verify(passwordEncoder, times(1)).matches(
            org.mockito.Mockito.anyString(),
            org.mockito.Mockito.anyString()
        )
    }
}
