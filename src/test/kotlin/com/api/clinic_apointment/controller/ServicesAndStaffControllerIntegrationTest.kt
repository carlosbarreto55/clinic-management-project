package com.api.clinic_apointment.controller

import com.api.clinic_apointment.config.JwtUtil
import com.api.clinic_apointment.entity.Service
import com.api.clinic_apointment.entity.Staff
import com.api.clinic_apointment.entity.User
import com.api.clinic_apointment.repository.ServiceRepository
import com.api.clinic_apointment.repository.StaffRepository
import com.api.clinic_apointment.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.containsString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ServicesAndStaffControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val serviceRepository: ServiceRepository,
    private val staffRepository: StaffRepository,
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil
) {
    private lateinit var token: String

    @BeforeEach
    fun setUp() {
        serviceRepository.deleteAll()
        staffRepository.deleteAll()
        userRepository.deleteAll()

        val user = userRepository.save(
            User(
                email = "staff-endpoints@clinic.com",
                passwordHash = "unused",
                role = "RECEPTIONIST"
            )
        )
        token = jwtUtil.generateToken(user.id, user.role)
    }

    @Test
    fun `GET services returns all services`() {
        serviceRepository.save(Service(name = "Consultation", price = BigDecimal("150.00"), durationMinutes = 30))
        serviceRepository.save(Service(name = "X-Ray", price = BigDecimal("300.00"), durationMinutes = 20))

        mockMvc.perform(get("/api/services").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
            .andExpect(jsonPath("$[*].id", hasSize<Any>(2)))
            .andExpect(jsonPath("$[*].name", containsInAnyOrder("Consultation", "X-Ray")))
            .andExpect(jsonPath("$[?(@.name == 'Consultation')].price").value(150.00))
            .andExpect(jsonPath("$[?(@.name == 'Consultation')].durationMinutes").value(30))
            .andExpect(jsonPath("$[?(@.name == 'X-Ray')].price").value(300.00))
            .andExpect(jsonPath("$[?(@.name == 'X-Ray')].durationMinutes").value(20))
    }

    @Test
    fun `GET staff returns all staff`() {
        staffRepository.save(Staff(name = "Dr. Smith", userId = 1L))
        staffRepository.save(Staff(name = "Dr. Jones", userId = 2L))

        mockMvc.perform(get("/api/staff").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(2)))
            .andExpect(jsonPath("$[*].id", hasSize<Any>(2)))
            .andExpect(jsonPath("$[*].name", containsInAnyOrder("Dr. Smith", "Dr. Jones")))
            .andExpect(jsonPath("$[*].userId").doesNotExist())
            .andExpect(content().string(not(containsString("userId"))))
    }

    @Test
    fun `GET services returns empty array when no services exist`() {
        mockMvc.perform(get("/api/services").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(0)))
    }

    @Test
    fun `GET staff returns empty array when no staff exist`() {
        mockMvc.perform(get("/api/staff").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(0)))
    }

    @Test
    fun `GET services allows any authenticated role because Phase 3 has no role restriction`() {
        val staffToken = jwtUtil.generateToken(999L, "STAFF")
        serviceRepository.save(Service(name = "Consultation", price = BigDecimal("150.00"), durationMinutes = 30))

        mockMvc.perform(get("/api/services").header("Authorization", "Bearer $staffToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
    }

    @Test
    fun `GET staff allows any authenticated role because Phase 3 has no role restriction`() {
        val patientToken = jwtUtil.generateToken(998L, "PATIENT")
        staffRepository.save(Staff(name = "Dr. Smith", userId = 1L))

        mockMvc.perform(get("/api/staff").header("Authorization", "Bearer $patientToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
    }

    @Test
    fun `GET services without token returns 401`() {
        mockMvc.perform(get("/api/services"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET services with invalid token returns 401`() {
        mockMvc.perform(get("/api/services").header("Authorization", "Bearer not-a-valid-jwt"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET staff without token returns 401`() {
        mockMvc.perform(get("/api/staff"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET staff with invalid token returns 401`() {
        mockMvc.perform(get("/api/staff").header("Authorization", "Bearer not-a-valid-jwt"))
            .andExpect(status().isUnauthorized)
    }
}
