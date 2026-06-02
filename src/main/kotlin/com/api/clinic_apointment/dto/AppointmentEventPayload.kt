package com.api.clinic_apointment.dto

import java.time.LocalDateTime

data class AppointmentEventPayload(
    val id: Long,
    val action: String,
    val clientName: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val status: String,
    val type: String,
    val serviceId: Long,
    val staffId: Long
)
