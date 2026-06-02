package com.api.clinic_apointment.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtUtil(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.expiration-ms}") private val expirationMs: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    fun generateToken(userId: Long, role: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
            .signWith(key)
            .compact()
    }

    fun extractUserId(token: String): Long = parseToken(token).subject.toLong()

    fun extractRole(token: String): String = parseToken(token)["role", String::class.java]

    fun isTokenValid(token: String): Boolean = try {
        parseToken(token)
        true
    } catch (e: io.jsonwebtoken.JwtException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    private fun parseToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
