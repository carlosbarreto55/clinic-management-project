package com.api.clinic_apointment.dto

import java.time.LocalDateTime

data class AppointmentResponse(
    val id: Long,
    val clientName: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val status: String,
    val type: String,
    val serviceId: Long,
    val staffId: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
