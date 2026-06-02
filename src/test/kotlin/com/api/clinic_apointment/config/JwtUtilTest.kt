package com.api.clinic_apointment.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JwtUtilTest {

    private val secret = "this-is-a-test-secret-key-that-is-long-enough-for-hs256"
    private val expirationMs: Long = 3600000L

    private lateinit var jwtUtil: JwtUtil

    @BeforeEach
    fun setUp() {
        jwtUtil = JwtUtil(secret, expirationMs)
    }

    @Test
    fun `generateToken returns non-blank JWT`() {
        val token = jwtUtil.generateToken(1L, "RECEPTIONIST")

        assertNotNull(token)
        assertTrue(token.isNotBlank())
        assertTrue(token.count { it == '.' } == 2)
    }

    @Test
    fun `generateToken and extractUserId should round-trip`() {
        val userId = 42L
        val role = "ADMIN"
        val token = jwtUtil.generateToken(userId, role)

        val extractedUserId = jwtUtil.extractUserId(token)

        assertEquals(userId, extractedUserId)
    }

    @Test
    fun `generateToken and extractRole should round-trip`() {
        val userId = 7L
        val role = "STAFF"
        val token = jwtUtil.generateToken(userId, role)

        val extractedRole = jwtUtil.extractRole(token)

        assertEquals(role, extractedRole)
    }

    @Test
    fun `isTokenValid returns true for valid token`() {
        val token = jwtUtil.generateToken(1L, "RECEPTIONIST")

        assertTrue(jwtUtil.isTokenValid(token))
    }

    @Test
    fun `isTokenValid returns false for tampered token`() {
        val token = jwtUtil.generateToken(1L, "RECEPTIONIST")
        val tamperedToken = token.substring(0, token.length - 1) +
            if (token.last() == 'A') 'B' else 'A'

        assertFalse(jwtUtil.isTokenValid(tamperedToken))
    }

    @Test
    fun `isTokenValid returns false for expired token`() {
        val expiredJwtUtil = JwtUtil(secret, -1000L)
        val token = expiredJwtUtil.generateToken(1L, "RECEPTIONIST")

        assertFalse(jwtUtil.isTokenValid(token))
    }

    @Test
    fun `isTokenValid returns false for completely malformed string`() {
        assertFalse(jwtUtil.isTokenValid("this-is-not-a-jwt"))
        assertFalse(jwtUtil.isTokenValid(""))
        assertFalse(jwtUtil.isTokenValid("   "))
    }

    @Test
    fun `extractUserId throws on malformed token`() {
        assertThrows(Exception::class.java) {
            jwtUtil.extractUserId("garbage-token")
        }
    }
}
