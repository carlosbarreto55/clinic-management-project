package com.api.clinic_apointment.dto

import java.math.BigDecimal

data class ServiceResponse(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val durationMinutes: Int
)
