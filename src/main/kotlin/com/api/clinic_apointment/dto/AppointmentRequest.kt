package com.api.clinic_apointment.dto

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class AppointmentRequest(
    @field:Size(max = 120, message = "clientName must be at most 120 characters")
    val clientName: String? = null,

    @field:NotNull(message = "startTime is required")
    @field:Future(message = "startTime must be in the future")
    val startTime: LocalDateTime?,

    val endTime: LocalDateTime? = null,

    @field:NotBlank(message = "type is required")
    @field:Size(max = 20, message = "type must be at most 20 characters")
    @field:Pattern(
        regexp = "APPOINTMENT|LOCK",
        message = "type must be APPOINTMENT or LOCK"
    )
    val type: String?,

    @field:Positive(message = "serviceId must be positive")
    val serviceId: Long? = null,

    @field:NotNull(message = "staffId is required")
    @field:Positive(message = "staffId must be positive")
    val staffId: Long?
)
