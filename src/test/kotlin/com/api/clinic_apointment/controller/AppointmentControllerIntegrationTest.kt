package com.api.clinic_apointment.controller

import com.api.clinic_apointment.config.JwtUtil
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppointmentControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val appointmentRepository: AppointmentRepository,
    private val serviceRepository: ServiceRepository,
    private val staffRepository: StaffRepository,
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil
) {
    private lateinit var receptionistToken: String
    private lateinit var staffToken: String
    private lateinit var service: Service
    private lateinit var staff: Staff

    @BeforeEach
    fun setUp() {
        appointmentRepository.deleteAll()
        staffRepository.deleteAll()
        serviceRepository.deleteAll()
        userRepository.deleteAll()

        val receptionist = userRepository.save(
            User(
                email = "appointments@clinic.com",
                passwordHash = "unused",
                role = "RECEPTIONIST"
            )
        )
        val staffUser = userRepository.save(
            User(
                email = "staff-appointments@clinic.com",
                passwordHash = "unused",
                role = "STAFF"
            )
        )
        receptionistToken = jwtUtil.generateToken(receptionist.id, receptionist.role)
        staffToken = jwtUtil.generateToken(staffUser.id, staffUser.role)
        service = serviceRepository.save(Service(name = "Consultation", price = BigDecimal("150.00"), durationMinutes = 30))
        staff = staffRepository.save(Staff(name = "Dr. Smith", userId = staffUser.id))
    }

    @Test
    fun `GET appointments returns active appointments in ISO datetime range`() {
        val start = futureStart()
        appointmentRepository.saveAll(
            listOf(
                appointment("In Range", start, start.plusMinutes(30)),
                appointment("Cancelled", start.plusMinutes(15), start.plusMinutes(45), status = "CANCELLED"),
                appointment("Out Of Range", start.plusDays(1), start.plusDays(1).plusMinutes(30))
            )
        )

        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .param("startDate", start.minusMinutes(5).toString())
                .param("endDate", start.plusHours(1).toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].clientName").value("In Range"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"))
    }

    @Test
    fun `GET appointments rejects missing date range`() {
        mockMvc.perform(get("/api/appointments").header("Authorization", "Bearer $receptionistToken"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").isString)
    }

    @Test
    fun `GET appointments rejects invalid date range`() {
        val start = futureStart()

        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .param("startDate", "not-a-date")
                .param("endDate", start.plusHours(1).toString())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").isString)
    }

    @Test
    fun `GET appointments rejects reversed date range`() {
        val start = futureStart()

        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .param("startDate", start.plusHours(1).toString())
                .param("endDate", start.toString())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("endDate must be on or after startDate"))
    }

    @Test
    fun `GET appointments rejects date range over max window`() {
        val start = futureStart()

        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .param("startDate", start.toString())
                .param("endDate", start.plusDays(31).plusHours(1).toString())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("date range must not exceed 31 days"))
    }

    @Test
    fun `GET appointments allows authenticated staff role because Phase 4 has no role restriction`() {
        val start = futureStart()
        appointmentRepository.save(appointment("Staff View", start, start.plusMinutes(30)))

        mockMvc.perform(
            get("/api/appointments")
                .header("Authorization", "Bearer $staffToken")
                .param("startDate", start.minusMinutes(5).toString())
                .param("endDate", start.plusHours(1).toString())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$", hasSize<Any>(1)))
    }

    @Test
    fun `POST appointments creates appointment with JWT`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(start = start))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.clientName").value("John Doe"))
            .andExpect(jsonPath("$.startTime").value(iso(start)))
            .andExpect(jsonPath("$.endTime").value(iso(start.plusMinutes(30))))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.type").value("APPOINTMENT"))
            .andExpect(jsonPath("$.serviceId").value(service.id))
            .andExpect(jsonPath("$.staffId").value(staff.id))
    }

    @Test
    fun `POST appointments rejects overlapping appointment for same staff`() {
        val start = futureStart()
        appointmentRepository.save(appointment("Existing Patient", start, start.plusMinutes(30)))

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(clientName = "Overlap Patient", start = start.plusMinutes(15), end = start.plusMinutes(45)))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Appointment overlaps an existing appointment"))
    }

    @Test
    fun `POST appointments rejects missing clientName for appointment`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(clientName = null, start = start))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("clientName is required for APPOINTMENT"))
    }

    @Test
    fun `POST appointments rejects missing serviceId for appointment`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(start = start, serviceId = null))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("serviceId is required"))
    }

    @Test
    fun `POST appointments rejects missing serviceId for lock`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(clientName = null, start = start, type = "LOCK", serviceId = null))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("serviceId is required"))
    }

    @Test
    fun `POST appointments rejects invalid type`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(start = start, type = "BLOCKED"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.details.type").value("type must be APPOINTMENT or LOCK"))
    }

    @Test
    fun `POST appointments rejects lowercase type`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(start = start, type = "appointment"))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.details.type").value("type must be APPOINTMENT or LOCK"))
    }

    @Test
    fun `POST appointments rejects oversized type`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(start = start, type = "A".repeat(21)))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.details.type").isString)
    }

    @Test
    fun `POST appointments rejects endTime equal to startTime`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(start = start, end = start))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("endTime must be after startTime"))
    }

    @Test
    fun `POST appointments rejects oversized clientName`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(clientName = "A".repeat(121), start = start))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation failed"))
            .andExpect(jsonPath("$.details.clientName").value("clientName must be at most 120 characters"))
    }

    @Test
    fun `POST appointments rejects duration over max appointment window`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(start = start, end = start.plusHours(9)))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("appointment duration must not exceed 8 hours"))
    }

    @Test
    fun `POST appointments rejects missing staff`() {
        val start = futureStart()

        mockMvc.perform(
            post("/api/appointments")
                .header("Authorization", "Bearer $receptionistToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(start = start, staffId = staff.id + 9999))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("staffId does not exist"))
    }

    @Test
    fun `GET appointment by id returns details`() {
        val start = futureStart()
        val saved = appointmentRepository.save(appointment("Single Patient", start, start.plusMinutes(30)))

        mockMvc.perform(get("/api/appointments/${saved.id}").header("Authorization", "Bearer $receptionistToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(saved.id))
            .andExpect(jsonPath("$.clientName").value("Single Patient"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `GET appointment by id returns 404 for missing appointment`() {
        mockMvc.perform(get("/api/appointments/999999").header("Authorization", "Bearer $receptionistToken"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Appointment not found"))
    }

    @Test
    fun `GET appointment by id rejects non-positive id`() {
        mockMvc.perform(get("/api/appointments/0").header("Authorization", "Bearer $receptionistToken"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation failed"))
    }

    @Test
    fun `DELETE appointment rejects non-positive id`() {
        mockMvc.perform(delete("/api/appointments/-1").header("Authorization", "Bearer $receptionistToken"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Validation failed"))
    }

    @Test
    fun `GET appointment by id returns 404 for cancelled appointment`() {
        val start = futureStart()
        val saved = appointmentRepository.save(appointment("Cancelled Patient", start, start.plusMinutes(30), status = "CANCELLED"))

        mockMvc.perform(get("/api/appointments/${saved.id}").header("Authorization", "Bearer $receptionistToken"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Appointment not found"))
    }

    @Test
    fun `DELETE appointment soft deletes and returns updated response`() {
        val start = futureStart()
        val saved = appointmentRepository.save(appointment("Cancel Patient", start, start.plusMinutes(30)))

        mockMvc.perform(delete("/api/appointments/${saved.id}").header("Authorization", "Bearer $receptionistToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(saved.id))
            .andExpect(jsonPath("$.clientName").value("Cancel Patient"))
            .andExpect(jsonPath("$.status").value("CANCELLED"))

        val cancelled = appointmentRepository.findById(saved.id).orElseThrow()
        assertEquals("CANCELLED", cancelled.status)
    }

    @Test
    fun `DELETE appointment returns 409 when already cancelled`() {
        val start = futureStart()
        val saved = appointmentRepository.save(appointment("Already Cancelled", start, start.plusMinutes(30), status = "CANCELLED"))

        mockMvc.perform(delete("/api/appointments/${saved.id}").header("Authorization", "Bearer $receptionistToken"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("Appointment is already cancelled"))
    }

    @Test
    fun `all appointment endpoints return 401 without JWT`() {
        val start = futureStart()
        val saved = appointmentRepository.save(appointment("Auth Patient", start, start.plusMinutes(30)))

        mockMvc.perform(
            get("/api/appointments")
                .param("startDate", start.minusMinutes(5).toString())
                .param("endDate", start.plusHours(1).toString())
        ).andExpect(status().isUnauthorized)

        mockMvc.perform(
            post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(appointmentBody(start = start.plusHours(1)))
        ).andExpect(status().isUnauthorized)

        mockMvc.perform(get("/api/appointments/${saved.id}"))
            .andExpect(status().isUnauthorized)

        mockMvc.perform(delete("/api/appointments/${saved.id}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `concurrent overlapping creates for same staff persist only one appointment`() {
        val start = futureStart()
        val ready = CountDownLatch(2)
        val startRequests = CountDownLatch(1)
        val statuses = ConcurrentLinkedQueue<Int>()
        val executor = Executors.newFixedThreadPool(2)

        try {
            repeat(2) { index ->
                executor.submit {
                    ready.countDown()
                    assertTrue(startRequests.await(5, TimeUnit.SECONDS))
                    val result = mockMvc.perform(
                        post("/api/appointments")
                            .header("Authorization", "Bearer $receptionistToken")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(appointmentBody(clientName = "Concurrent Patient $index", start = start, end = start.plusMinutes(30)))
                    ).andReturn()
                    statuses.add(result.response.status)
                }
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS))
            startRequests.countDown()
            executor.shutdown()
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }

        assertEquals(listOf(201, 409), statuses.sorted())
        assertEquals(1, appointmentRepository.findActiveInRange(start.minusMinutes(1), start.plusMinutes(31)).size)
    }

    private fun appointmentBody(
        clientName: String? = "John Doe",
        start: LocalDateTime,
        end: LocalDateTime? = null,
        type: String = "APPOINTMENT",
        serviceId: Long? = service.id,
        staffId: Long? = staff.id
    ): String {
        val body = mutableMapOf<String, Any?>(
            "clientName" to clientName,
            "startTime" to start.toString(),
            "type" to type,
            "serviceId" to serviceId,
            "staffId" to staffId
        )
        if (end != null) {
            body["endTime"] = end.toString()
        }
        return objectMapper.writeValueAsString(body)
    }

    private fun appointment(
        clientName: String?,
        start: LocalDateTime,
        end: LocalDateTime,
        status: String = "ACTIVE",
        type: String = "APPOINTMENT"
    ) = Appointment(
        clientName = clientName,
        startTime = start,
        endTime = end,
        status = status,
        type = type,
        serviceId = service.id,
        staffId = staff.id
    )

    private fun iso(value: LocalDateTime): String = value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

    private fun futureStart(): LocalDateTime = LocalDateTime.now()
        .plusDays(30)
        .withHour(9)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
}
