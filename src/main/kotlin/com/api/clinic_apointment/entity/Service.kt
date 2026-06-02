package com.api.clinic_apointment.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "services")
data class Service(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(nullable = false, precision = 10, scale = 2)
    val price: BigDecimal,

    @Column(name = "duration_minutes", nullable = false)
    val durationMinutes: Int
)
