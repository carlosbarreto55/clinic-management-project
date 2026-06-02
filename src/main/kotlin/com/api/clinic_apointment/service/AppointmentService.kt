package com.api.clinic_apointment.service

import com.api.clinic_apointment.dto.AppointmentEventPayload
import com.api.clinic_apointment.dto.AppointmentRequest
import com.api.clinic_apointment.dto.AppointmentResponse
import com.api.clinic_apointment.entity.Appointment
import com.api.clinic_apointment.event.AppointmentEvent
import com.api.clinic_apointment.repository.AppointmentRepository
import com.api.clinic_apointment.repository.ServiceRepository
import com.api.clinic_apointment.repository.StaffRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val serviceRepository: ServiceRepository,
    private val staffRepository: StaffRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional(readOnly = true)
    fun findAppointments(startDate: LocalDate, endDate: LocalDate): List<AppointmentResponse> {
        if (endDate.isBefore(startDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate")
        }
        if (startDate.plusDays(MAX_DATE_RANGE_DAYS).isBefore(endDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "date range must not exceed $MAX_DATE_RANGE_DAYS days")
        }

        val start = startDate.atStartOfDay()
        val endExclusive = endDate.plusDays(1).atStartOfDay()
        return appointmentRepository.findActiveInRange(start, endExclusive).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun findAppointments(startDate: LocalDateTime, endDate: LocalDateTime): List<AppointmentResponse> {
        if (endDate.isBefore(startDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate")
        }
        if (Duration.between(startDate, endDate) > MAX_DATE_RANGE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "date range must not exceed $MAX_DATE_RANGE_DAYS days")
        }

        return appointmentRepository.findActiveInRange(startDate, endDate).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getAppointment(id: Long): AppointmentResponse {
        return appointmentRepository.findById(id)
            .filter { it.status != CANCELLED }
            .map { it.toResponse() }
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found") }
    }

    @Transactional
    fun createAppointment(request: AppointmentRequest): AppointmentResponse {
        val startTime = request.startTime ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime is required")
        val type = request.type ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required")
        val staffId = request.staffId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "staffId is required")

        if (type !in setOf(APPOINTMENT, LOCK)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be APPOINTMENT or LOCK")
        }
        if (type == APPOINTMENT && request.clientName.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "clientName is required for APPOINTMENT")
        }
        if (!staffRepository.existsById(staffId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "staffId does not exist")
        }
        staffRepository.findByIdForUpdate(staffId)

        val serviceId = request.serviceId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceId is required")
        val service = serviceRepository.findById(serviceId)
            .orElseThrow { ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceId does not exist") }
        val endTime = request.endTime ?: startTime.plusMinutes(service.durationMinutes.toLong())

        if (!endTime.isAfter(startTime)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "endTime must be after startTime")
        }
        if (Duration.between(startTime, endTime) > MAX_APPOINTMENT_DURATION) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "appointment duration must not exceed ${MAX_APPOINTMENT_DURATION.toHours()} hours")
        }
        if (appointmentRepository.findOverlappingForUpdate(staffId, startTime, endTime).isNotEmpty()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Appointment overlaps an existing appointment")
        }

        val saved = appointmentRepository.save(
            Appointment(
                clientName = request.clientName,
                startTime = startTime,
                endTime = endTime,
                status = ACTIVE,
                type = type,
                serviceId = serviceId,
                staffId = staffId
            )
        )
        eventPublisher.publishEvent(AppointmentEvent(saved.toEventPayload("CREATED")))
        return saved.toResponse()
    }

    @Transactional
    fun cancelAppointment(id: Long): AppointmentResponse {
        val appointment = appointmentRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found") }

        if (appointment.status == CANCELLED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Appointment is already cancelled")
        }

        val cancelled = appointmentRepository.save(appointment.copy(status = CANCELLED, updatedAt = LocalDateTime.now()))
        eventPublisher.publishEvent(AppointmentEvent(cancelled.toEventPayload("CANCELLED")))
        return cancelled.toResponse()
    }

    private fun Appointment.toResponse() = AppointmentResponse(
        id = id,
        clientName = clientName,
        startTime = startTime,
        endTime = endTime,
        status = status,
        type = type,
        serviceId = serviceId,
        staffId = staffId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Appointment.toEventPayload(action: String) = AppointmentEventPayload(
        id = id,
        action = action,
        clientName = clientName,
        startTime = startTime,
        endTime = endTime,
        status = status,
        type = type,
        serviceId = serviceId,
        staffId = staffId
    )

    companion object {
        private const val ACTIVE = "ACTIVE"
        private const val APPOINTMENT = "APPOINTMENT"
        private const val LOCK = "LOCK"
        private const val CANCELLED = "CANCELLED"
        private const val MAX_DATE_RANGE_DAYS = 31L
        private val MAX_DATE_RANGE = Duration.ofDays(MAX_DATE_RANGE_DAYS)
        private val MAX_APPOINTMENT_DURATION = Duration.ofHours(8)
    }
}
