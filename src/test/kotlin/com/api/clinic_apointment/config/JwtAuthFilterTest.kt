package com.api.clinic_apointment.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthFilterTest {

    private val jwtUtil = mock(JwtUtil::class.java)
    private val filter = JwtAuthFilter(jwtUtil)

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `doFilterInternal sets SecurityContext for valid token`() {
        `when`(jwtUtil.isTokenValid("valid.jwt.token")).thenReturn(true)
        `when`(jwtUtil.extractUserId("valid.jwt.token")).thenReturn(1L)
        `when`(jwtUtil.extractRole("valid.jwt.token")).thenReturn("STAFF")

        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer valid.jwt.token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertNotNull(auth)
        assertEquals("1", auth.principal)
        assertTrue(auth.authorities.any { it.authority == "ROLE_STAFF" })
    }

    @Test
    fun `doFilterInternal does not set SecurityContext for missing Authorization header`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `doFilterInternal does not set SecurityContext for invalid token`() {
        `when`(jwtUtil.isTokenValid("invalid.token")).thenReturn(false)

        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer invalid.token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `doFilterInternal does not set SecurityContext for malformed token`() {
        `when`(jwtUtil.isTokenValid("malformed.jwt")).thenReturn(true)
        `when`(jwtUtil.extractUserId("malformed.jwt")).thenThrow(io.jsonwebtoken.JwtException("malformed"))

        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer malformed.jwt")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `doFilterInternal calls filterChain doFilter in all cases`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer valid.jwt.token")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        `when`(jwtUtil.isTokenValid("valid.jwt.token")).thenReturn(true)
        `when`(jwtUtil.extractUserId("valid.jwt.token")).thenReturn(1L)
        `when`(jwtUtil.extractRole("valid.jwt.token")).thenReturn("STAFF")

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
    }
}
