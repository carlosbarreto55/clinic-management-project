package com.api.clinic_apointment.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 255)
    val email: String,

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    val passwordHash: String,

    @Column(nullable = false, length = 50)
    val role: String
)
