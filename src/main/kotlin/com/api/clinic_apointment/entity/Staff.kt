package com.api.clinic_apointment.entity

import jakarta.persistence.*

@Entity
@Table(name = "staff")
data class Staff(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long
)
