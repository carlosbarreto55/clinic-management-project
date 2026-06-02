package com.api.clinic_apointment.controller

import com.api.clinic_apointment.dto.AppointmentRequest
import com.api.clinic_apointment.dto.AppointmentResponse
import com.api.clinic_apointment.service.AppointmentService
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/appointments")
@Validated
class AppointmentController(private val appointmentService: AppointmentService) {
    @GetMapping
    fun listAppointments(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startDate: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endDate: LocalDateTime
    ): List<AppointmentResponse> = appointmentService.findAppointments(startDate, endDate)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAppointment(@Valid @RequestBody request: AppointmentRequest): AppointmentResponse {
        return appointmentService.createAppointment(request)
    }

    @GetMapping("/{id}")
    fun getAppointment(@Positive @PathVariable id: Long): AppointmentResponse = appointmentService.getAppointment(id)

    @DeleteMapping("/{id}")
    fun deleteAppointment(@Positive @PathVariable id: Long): AppointmentResponse = appointmentService.cancelAppointment(id)
}
