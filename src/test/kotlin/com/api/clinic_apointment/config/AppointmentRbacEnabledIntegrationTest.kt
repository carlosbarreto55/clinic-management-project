package com.api.clinic_apointment.config

import com.api.clinic_apointment.entity.Appointment
import com.api.clinic_apointment.entity.Service
import com.api.clinic_apointment.entity.Staff
import com.api.clinic_apointment.entity.User
import com.api.clinic_apointment.repository.AppointmentRepository
import com.api.clinic_apointment.repository.ServiceRepository
import com.api.clinic_apointment.repository.StaffRepository
import com.api.clinic_apointment.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest(properties = ["security.appointments.rbac-enabled=true"])
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppointmentRbacEnabledIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val appointmentRepository: AppointmentRepository,
    private val serviceRepository: ServiceRepository,
    private val staffRepository: StaffRepository,
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil
) {
    private lateinit var adminToken: String
    private lateinit var receptionistToken: String
    private lateinit var staffToken: String
    private lateinit var service: Service
    private lateinit var staff: Staff
    private lateinit var savedAppointment: Appointment

    @BeforeEach
    fun setUp() {
        appointmentRepository.deleteAll()
        staffRepository.deleteAll()
        serviceRepository.deleteAll()
        userRepository.deleteAll()

        val admin = userRepository.save(User(email = "admin-rbac@clinic.com", passwordHash = "unused", role = "ADMIN"))
        val receptionist = userRepository.save(
            User(email = "receptionist-rbac@clinic.com", passwordHash = "unused", role = "RECEPTIONIST")
        )
        val staffUser = userRepository.save(User(email = "staff-rbac@clinic.com", passwordHash = "unused", role = "STAFF"))

        adminToken = jwtUtil.generateToken(admin.id, admin.role)
        receptionistToken = jwtUtil.generateToken(receptionist.id, receptionist.role)
        staffToken = jwtUtil.generateToken(staffUser.id, staffUser.role)

        service = serviceRepository.save(Service(name = "Consultation", price = BigDecimal("150.00"), durationMinutes = 30))
        staff = staffRepository.save(Staff(name = "Dr. RBAC", userId = staffUser.id))
        val start = futureStart()
        savedAppointment = appointmentRepository.save(appointment("RBAC Patient", start, start.plusMinutes(30)))
    }

    @Test
    fun `ADMIN can access GET appointments when appointment RBAC flag is enabled`() {
        val start = savedAppointment.startTime

        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer $adminToken")
                .param("startDate", start.minusMinutes(5).toString())
                .param("endDate", start.plusHours(1).toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
    }

    @Test
    fun `RECEPTIONIST can access GET appointments when appointment RBAC flag is enabled`() {
        val start = savedAppointment.startTime

        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .param("startDate", start.minusMinutes(5).toString())
                .param("endDate", start.plusHours(1).toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
    }

    @Test
    fun `STAFF receives 403 for GET appointments when appointment RBAC flag is enabled`() {
        val start = savedAppointment.startTime

        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer $staffToken")
                .param("startDate", start.minusMinutes(5).toString())
                .param("endDate", start.plusHours(1).toString())
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `STAFF receives 403 for POST appointments when appointment RBAC flag is enabled`() {
        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(futureStart().plusHours(1)))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `STAFF receives 403 for GET appointment by id when appointment RBAC flag is enabled`() {
        mockMvc.perform(
            get("/api/appointments/${savedAppointment.id}")
                .header("Authorization", "Bearer $staffToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `STAFF receives 403 for DELETE appointment when appointment RBAC flag is enabled`() {
        mockMvc.perform(
            delete("/api/appointments/${savedAppointment.id}")
                .header("Authorization", "Bearer $staffToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `unauthenticated users receive 401 not 403 when appointment RBAC flag is enabled`() {
        val start = savedAppointment.startTime

        mockMvc.perform(
            get("/api/appointments")
                .param("startDate", start.minusMinutes(5).toString())
                .param("endDate", start.plusHours(1).toString())
        )
            .andExpect(status().isUnauthorized)
    }

    private fun appointmentBody(start: LocalDateTime): String = objectMapper.writeValueAsString(
        mapOf(
            "clientName" to "RBAC Create Patient",
            "startTime" to start.toString(),
            "type" to "APPOINTMENT",
            "serviceId" to service.id,
            "staffId" to staff.id
        )
    )

    private fun appointment(clientName: String, start: LocalDateTime, end: LocalDateTime) = Appointment(
        clientName = clientName,
        startTime = start,
        endTime = end,
        status = "ACTIVE",
        type = "APPOINTMENT",
        serviceId = service.id,
        staffId = staff.id
    )

    private fun futureStart(): LocalDateTime = LocalDateTime.now()
        .plusDays(30)
        .withHour(9)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
}
