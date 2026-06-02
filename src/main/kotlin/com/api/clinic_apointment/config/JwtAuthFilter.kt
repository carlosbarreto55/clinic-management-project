package com.api.clinic_apointment.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val jwtUtil: JwtUtil) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            if (jwtUtil.isTokenValid(token)) {
                try {
                    val userId = jwtUtil.extractUserId(token)
                    val role = jwtUtil.extractRole(token)
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
                    val authentication = UsernamePasswordAuthenticationToken(
                        userId.toString(), null, authorities
                    )
                    SecurityContextHolder.getContext().authentication = authentication
                } catch (e: io.jsonwebtoken.JwtException) {
                    logger.debug("JWT validation failed", e)
                } catch (e: IllegalArgumentException) {
                    logger.debug("JWT validation failed", e)
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
