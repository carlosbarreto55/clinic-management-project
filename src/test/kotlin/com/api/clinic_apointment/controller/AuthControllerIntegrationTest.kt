package com.api.clinic_apointment.controller

import com.api.clinic_apointment.entity.User
import com.api.clinic_apointment.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.containsString
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AuthControllerIntegrationTest @Autowired constructor(
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
            role = "RECEPTIONIST"
        )
        userRepository.save(user)
    }

    @Test
    fun `POST to login with valid credentials returns 200 with token`() {
        val loginRequest = """{"email":"test@clinic.com","password":"password123"}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isString)
            .andExpect(jsonPath("$.token").isNotEmpty)
    }

    @Test
    fun `POST to login with wrong password returns 401`() {
        val loginRequest = """{"email":"test@clinic.com","password":"wrongPassword"}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST to login with unknown email returns 401`() {
        val loginRequest = """{"email":"unknown@clinic.com","password":"password123"}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST to login with blank email returns 400`() {
        val loginRequest = """{"email":"","password":"password123"}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST to login with blank password returns 400`() {
        val loginRequest = """{"email":"test@clinic.com","password":""}"""

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET to appointments without token returns 401`() {
        mockMvc.perform(get("/api/appointments"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET to appointments with invalid token returns 401`() {
        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer invalid-token-here")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET to appointments with valid token does not return 401`() {
        val loginRequest = """{"email":"test@clinic.com","password":"password123"}"""

        val loginResponse = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequest)
        )
            .andExpect(status().isOk)
            .andReturn()

        val responseBody = loginResponse.response.contentAsString
        val token = objectMapper.readTree(responseBody).get("token").asText()

        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().`is`(org.hamcrest.Matchers.not(401)))
    }
}
