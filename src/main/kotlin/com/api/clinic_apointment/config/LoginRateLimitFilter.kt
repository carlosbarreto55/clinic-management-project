package com.api.clinic_apointment.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class LoginRateLimitFilter(private val loginRateLimiter: LoginRateLimiter) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response)
            return
        }

        val key = request.remoteAddr ?: "unknown"
        if (loginRateLimiter.isBlocked(key)) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many failed login attempts")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isLoginRequest(request: HttpServletRequest): Boolean {
        return request.method.equals("POST", ignoreCase = true) && request.requestURI == "${request.contextPath}/api/auth/login"
    }
}
