package com.api.clinic_apointment.service

import com.api.clinic_apointment.dto.AppointmentRequest
import com.api.clinic_apointment.entity.Appointment
import com.api.clinic_apointment.entity.Service
import com.api.clinic_apointment.entity.Staff
import com.api.clinic_apointment.event.AppointmentEvent
import com.api.clinic_apointment.repository.AppointmentRepository
import com.api.clinic_apointment.repository.ServiceRepository
import com.api.clinic_apointment.repository.StaffRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class AppointmentServiceTest {
    private val appointmentRepository = mock(AppointmentRepository::class.java)
    private val serviceRepository = mock(ServiceRepository::class.java)
    private val staffRepository = mock(StaffRepository::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val appointmentService = AppointmentService(
        appointmentRepository,
        serviceRepository,
        staffRepository,
        eventPublisher
    )

    private val service = Service(id = 10L, name = "Consultation", price = BigDecimal("150.00"), durationMinutes = 30)
    private val start = LocalDateTime.of(2026, 6, 3, 9, 0)
    private val end = start.plusMinutes(30)
    private val authenticatedUserId = 100L
    private val otherAuthenticatedUserId = 200L

    @BeforeEach
    fun resetMocks() {
        reset(appointmentRepository, serviceRepository, staffRepository, eventPublisher)
    }

    @Test
    fun `createAppointment creates active appointment and derives endTime from service duration`() {
        val saved = activeAppointment(id = 20L)
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(serviceRepository.findById(service.id)).thenReturn(Optional.of(service))
        `when`(appointmentRepository.findOverlappingForUpdate(30L, start, end)).thenReturn(emptyList())
        `when`(appointmentRepository.save(any(Appointment::class.java))).thenReturn(saved)

        val response = appointmentService.createAppointment(validRequest(endTime = null), authenticatedUserId)

        val appointmentCaptor = ArgumentCaptor.forClass(Appointment::class.java)
        verify(appointmentRepository).save(appointmentCaptor.capture())
        assertEquals("John Doe", appointmentCaptor.value.clientName)
        assertEquals(start, appointmentCaptor.value.startTime)
        assertEquals(end, appointmentCaptor.value.endTime)
        assertEquals("ACTIVE", appointmentCaptor.value.status)
        assertEquals("APPOINTMENT", appointmentCaptor.value.type)
        assertEquals(service.id, appointmentCaptor.value.serviceId)
        assertEquals(30L, appointmentCaptor.value.staffId)

        assertEquals(saved.id, response.id)
        assertEquals("John Doe", response.clientName)
        assertEquals("ACTIVE", response.status)
    }

    @Test
    fun `createAppointment publishes created event after successful save`() {
        val saved = activeAppointment(id = 20L)
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(serviceRepository.findById(service.id)).thenReturn(Optional.of(service))
        `when`(appointmentRepository.findOverlappingForUpdate(30L, start, end)).thenReturn(emptyList())
        `when`(appointmentRepository.save(any(Appointment::class.java))).thenReturn(saved)

        appointmentService.createAppointment(validRequest(), authenticatedUserId)

        val captor = ArgumentCaptor.forClass(AppointmentEvent::class.java)
        verify(eventPublisher).publishEvent(captor.capture())
        assertEquals("CREATED", captor.value.payload.action)
        assertEquals(saved.id, captor.value.payload.id)
        assertEquals(saved.clientName, captor.value.payload.clientName)
        assertEquals(saved.status, captor.value.payload.status)
    }

    @Test
    fun `createAppointment rejects overlapping appointment with conflict`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(serviceRepository.findById(service.id)).thenReturn(Optional.of(service))
        `when`(appointmentRepository.findOverlappingForUpdate(30L, start, end)).thenReturn(listOf(activeAppointment(id = 99L)))

        val ex = assertThrows<ResponseStatusException> { appointmentService.createAppointment(validRequest(), authenticatedUserId) }

        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
        verify(eventPublisher, never()).publishEvent(any(AppointmentEvent::class.java))
    }

    @Test
    fun `createAppointment rejects missing staff`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(false)

        val ex = assertThrows<ResponseStatusException> { appointmentService.createAppointment(validRequest(), authenticatedUserId) }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `createAppointment rejects missing service for appointment`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(serviceRepository.findById(service.id)).thenReturn(Optional.empty())

        val ex = assertThrows<ResponseStatusException> { appointmentService.createAppointment(validRequest(), authenticatedUserId) }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `createAppointment rejects missing serviceId for lock`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(appointmentRepository.findOverlappingForUpdate(30L, start, end)).thenReturn(emptyList())
        `when`(appointmentRepository.save(any(Appointment::class.java))).thenReturn(activeAppointment(id = 20L))

        val ex = assertThrows<ResponseStatusException> {
            appointmentService.createAppointment(
                AppointmentRequest(
                    clientName = null,
                    startTime = start,
                    endTime = end,
                    type = "LOCK",
                    serviceId = null,
                    staffId = 30L
                ),
                authenticatedUserId
            )
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `createAppointment rejects invalid type`() {
        val ex = assertThrows<ResponseStatusException> {
            appointmentService.createAppointment(validRequest(type = "BLOCKED"), authenticatedUserId)
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `createAppointment rejects lowercase type`() {
        val ex = assertThrows<ResponseStatusException> {
            appointmentService.createAppointment(validRequest(type = "appointment"), authenticatedUserId)
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `createAppointment rejects missing clientName for appointment`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(serviceRepository.findById(service.id)).thenReturn(Optional.of(service))
        `when`(appointmentRepository.findOverlappingForUpdate(30L, start, end)).thenReturn(emptyList())
        `when`(appointmentRepository.save(any(Appointment::class.java))).thenReturn(activeAppointment(id = 20L))

        val ex = assertThrows<ResponseStatusException> {
            appointmentService.createAppointment(validRequest(clientName = null), authenticatedUserId)
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `createAppointment rejects blank clientName for appointment`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(serviceRepository.findById(service.id)).thenReturn(Optional.of(service))
        `when`(appointmentRepository.findOverlappingForUpdate(30L, start, end)).thenReturn(emptyList())
        `when`(appointmentRepository.save(any(Appointment::class.java))).thenReturn(activeAppointment(id = 20L))

        val ex = assertThrows<ResponseStatusException> {
            appointmentService.createAppointment(validRequest(clientName = "   "), authenticatedUserId)
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `createAppointment rejects endTime equal to startTime`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(serviceRepository.findById(service.id)).thenReturn(Optional.of(service))

        val ex = assertThrows<ResponseStatusException> { appointmentService.createAppointment(validRequest(endTime = start), authenticatedUserId) }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `createAppointment rejects endTime before startTime`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(serviceRepository.findById(service.id)).thenReturn(Optional.of(service))

        val ex = assertThrows<ResponseStatusException> {
            appointmentService.createAppointment(validRequest(endTime = start.minusMinutes(1)), authenticatedUserId)
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `getAppointment returns active appointment by id`() {
        `when`(appointmentRepository.findById(20L)).thenReturn(Optional.of(activeAppointment(id = 20L)))

        val response = appointmentService.getAppointment(20L)

        assertEquals(20L, response.id)
        assertEquals("John Doe", response.clientName)
        assertEquals("ACTIVE", response.status)
    }

    @Test
    fun `getAppointment returns not found for missing appointment`() {
        `when`(appointmentRepository.findById(20L)).thenReturn(Optional.empty())

        val ex = assertThrows<ResponseStatusException> { appointmentService.getAppointment(20L) }

        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }

    @Test
    fun `getAppointment returns not found for cancelled appointment`() {
        `when`(appointmentRepository.findById(20L)).thenReturn(Optional.of(activeAppointment(id = 20L, status = "CANCELLED")))

        val ex = assertThrows<ResponseStatusException> { appointmentService.getAppointment(20L) }

        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }

    @Test
    fun `findAppointments returns active appointments in date range`() {
        val active = activeAppointment(id = 20L)
        val startDate = LocalDate.of(2026, 6, 3)
        val endDate = LocalDate.of(2026, 6, 4)
        `when`(appointmentRepository.findActiveInRange(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()))
            .thenReturn(listOf(active))

        val appointments = appointmentService.findAppointments(startDate, endDate)

        assertEquals(1, appointments.size)
        assertEquals(active.id, appointments.first().id)
    }

    @Test
    fun `findAppointments rejects reversed date range`() {
        val ex = assertThrows<ResponseStatusException> {
            appointmentService.findAppointments(LocalDate.of(2026, 6, 4), LocalDate.of(2026, 6, 3))
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `findAppointments rejects date ranges over max window`() {
        val ex = assertThrows<ResponseStatusException> {
            appointmentService.findAppointments(start, start.plusDays(31).plusHours(1))
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
    }

    @Test
    fun `createAppointment rejects duration over max appointment window`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(serviceRepository.findById(service.id)).thenReturn(Optional.of(service))

        val ex = assertThrows<ResponseStatusException> {
            appointmentService.createAppointment(validRequest(endTime = start.plusHours(9)), authenticatedUserId)
        }

        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `createAppointment allows authenticated staff to create appointment for own staffId`() {
        val saved = activeAppointment(id = 20L)
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(staffRepository.existsById(30L)).thenReturn(true)
        `when`(serviceRepository.findById(service.id)).thenReturn(Optional.of(service))
        `when`(appointmentRepository.findOverlappingForUpdate(30L, start, end)).thenReturn(emptyList())
        `when`(appointmentRepository.save(any(Appointment::class.java))).thenReturn(saved)

        val response = appointmentService.createAppointment(validRequest(), authenticatedUserId)

        assertEquals(saved.id, response.id)
        verify(appointmentRepository).save(any(Appointment::class.java))
    }

    @Test
    fun `createAppointment rejects authenticated staff creating appointment for another staffId`() {
        allowOwner(otherAuthenticatedUserId, staffId = 40L)

        val ex = assertThrows<ResponseStatusException> {
            appointmentService.createAppointment(validRequest(), otherAuthenticatedUserId)
        }

        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
        verify(eventPublisher, never()).publishEvent(any(AppointmentEvent::class.java))
    }

    @Test
    fun `createAppointment rejects authenticated user without staff mapping`() {
        `when`(staffRepository.findByUserId(authenticatedUserId)).thenReturn(Optional.empty())

        val ex = assertThrows<ResponseStatusException> {
            appointmentService.createAppointment(validRequest(), authenticatedUserId)
        }

        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
        verify(eventPublisher, never()).publishEvent(any(AppointmentEvent::class.java))
    }

    @Test
    fun `cancelAppointment soft deletes appointment and publishes cancelled event`() {
        val appointment = activeAppointment(id = 20L)
        allowOwner(authenticatedUserId, staffId = appointment.staffId)
        `when`(appointmentRepository.findById(20L)).thenReturn(Optional.of(appointment))
        `when`(appointmentRepository.save(any(Appointment::class.java))).thenAnswer { it.arguments[0] }

        appointmentService.cancelAppointment(20L, authenticatedUserId)

        val appointmentCaptor = ArgumentCaptor.forClass(Appointment::class.java)
        verify(appointmentRepository).save(appointmentCaptor.capture())
        assertEquals("CANCELLED", appointmentCaptor.value.status)

        val eventCaptor = ArgumentCaptor.forClass(AppointmentEvent::class.java)
        verify(eventPublisher).publishEvent(eventCaptor.capture())
        assertEquals("CANCELLED", eventCaptor.value.payload.action)
        assertEquals("CANCELLED", eventCaptor.value.payload.status)
    }

    @Test
    fun `cancelAppointment rejects missing appointment with not found`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(appointmentRepository.findById(20L)).thenReturn(Optional.empty())

        val ex = assertThrows<ResponseStatusException> { appointmentService.cancelAppointment(20L, authenticatedUserId) }

        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
    }

    @Test
    fun `cancelAppointment rejects already cancelled appointment with conflict`() {
        allowOwner(authenticatedUserId, staffId = 30L)
        `when`(appointmentRepository.findById(20L)).thenReturn(Optional.of(activeAppointment(id = 20L, status = "CANCELLED")))

        val ex = assertThrows<ResponseStatusException> { appointmentService.cancelAppointment(20L, authenticatedUserId) }

        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
        verify(eventPublisher, never()).publishEvent(any(AppointmentEvent::class.java))
    }

    @Test
    fun `cancelAppointment allows authenticated staff to cancel own appointment`() {
        val appointment = activeAppointment(id = 20L)
        allowOwner(authenticatedUserId, staffId = appointment.staffId)
        `when`(appointmentRepository.findById(20L)).thenReturn(Optional.of(appointment))
        `when`(appointmentRepository.save(any(Appointment::class.java))).thenAnswer { it.arguments[0] }

        val response = appointmentService.cancelAppointment(20L, authenticatedUserId)

        assertEquals("CANCELLED", response.status)
        verify(appointmentRepository).save(any(Appointment::class.java))
    }

    @Test
    fun `cancelAppointment rejects authenticated staff cancelling another staff appointment`() {
        val appointment = activeAppointment(id = 20L, staffId = 30L)
        allowOwner(otherAuthenticatedUserId, staffId = 40L)
        `when`(appointmentRepository.findById(20L)).thenReturn(Optional.of(appointment))

        val ex = assertThrows<ResponseStatusException> {
            appointmentService.cancelAppointment(20L, otherAuthenticatedUserId)
        }

        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
        verify(eventPublisher, never()).publishEvent(any(AppointmentEvent::class.java))
    }

    @Test
    fun `cancelAppointment rejects authenticated user without staff mapping`() {
        `when`(staffRepository.findByUserId(authenticatedUserId)).thenReturn(Optional.empty())
        `when`(appointmentRepository.findById(20L)).thenReturn(Optional.of(activeAppointment(id = 20L)))

        val ex = assertThrows<ResponseStatusException> {
            appointmentService.cancelAppointment(20L, authenticatedUserId)
        }

        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        verify(appointmentRepository, never()).save(any(Appointment::class.java))
        verify(eventPublisher, never()).publishEvent(any(AppointmentEvent::class.java))
    }

    private fun allowOwner(userId: Long, staffId: Long) {
        `when`(staffRepository.findByUserId(userId)).thenReturn(Optional.of(staff(staffId, userId)))
    }

    private fun staff(id: Long, userId: Long) = Staff(id = id, name = "Dr. Owner $id", userId = userId)

    private fun validRequest(
        clientName: String? = "John Doe",
        type: String = "APPOINTMENT",
        endTime: LocalDateTime? = end
    ) = AppointmentRequest(
        clientName = clientName,
        startTime = start,
        endTime = endTime,
        type = type,
        serviceId = service.id,
        staffId = 30L
    )

    private fun activeAppointment(id: Long, status: String = "ACTIVE", staffId: Long = 30L) = Appointment(
        id = id,
        clientName = "John Doe",
        startTime = start,
        endTime = end,
        status = status,
        type = "APPOINTMENT",
        serviceId = service.id,
        staffId = staffId
    )
}
