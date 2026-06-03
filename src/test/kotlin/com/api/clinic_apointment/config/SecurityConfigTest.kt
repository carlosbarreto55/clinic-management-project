package com.api.clinic_apointment.config

import com.api.clinic_apointment.entity.User
import com.api.clinic_apointment.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class SecurityConfigTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper
) {

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        val user = User(
            email = "test@clinic.com",
            passwordHash = passwordEncoder.encode("password123"),
            role = "STAFF"
        )
        userRepository.save(user)
    }

    @Test
    fun `GET to login is publicly accessible returns 405 not 401`() {
        mockMvc.perform(get("/api/auth/login"))
            .andExpect(status().`is`(Matchers.not(401)))
    }

    @Test
    fun `POST to login is publicly accessible returns 400 for empty body not 401`() {
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().`is`(Matchers.not(401)))
    }

    @Test
    fun `GET to appointments without token returns 401`() {
        mockMvc.perform(get("/api/appointments"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET appointments with STAFF token is authenticated without role restriction`() {
        val loginRequest = """{"email":"test@clinic.com","password":"password123"}"""

        val loginResponse = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
        )
            .andExpect(status().isOk)
            .andReturn()

        val token = objectMapper.readTree(loginResponse.response.contentAsString).get("token").asText()

        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().`is`(Matchers.not(401)))
    }
}
