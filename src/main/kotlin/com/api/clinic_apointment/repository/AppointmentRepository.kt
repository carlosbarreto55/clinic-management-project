package com.api.clinic_apointment.repository

import com.api.clinic_apointment.entity.Appointment
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AppointmentRepository : JpaRepository<Appointment, Long> {

    @Query("""
        SELECT COUNT(a) FROM Appointment a
        WHERE a.staffId = :staffId
          AND a.status != 'CANCELLED'
          AND a.startTime < :newEndTime
          AND a.endTime > :newStartTime
    """)
    fun countOverlapping(
        @Param("staffId") staffId: Long,
        @Param("newStartTime") newStartTime: LocalDateTime,
        @Param("newEndTime") newEndTime: LocalDateTime
    ): Long

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM Appointment a
        WHERE a.staffId = :staffId
          AND a.status != 'CANCELLED'
          AND a.startTime < :newEndTime
          AND a.endTime > :newStartTime
    """)
    fun findOverlappingForUpdate(
        @Param("staffId") staffId: Long,
        @Param("newStartTime") newStartTime: LocalDateTime,
        @Param("newEndTime") newEndTime: LocalDateTime
    ): List<Appointment>

    @Query("""
        SELECT a FROM Appointment a
        WHERE a.status != 'CANCELLED'
          AND a.startTime >= :start
          AND a.startTime < :endExclusive
        ORDER BY a.startTime ASC
    """)
    fun findActiveInRange(
        @Param("start") start: LocalDateTime,
        @Param("endExclusive") endExclusive: LocalDateTime
    ): List<Appointment>

    fun findByStartTimeBetweenAndStatusNot(
        start: LocalDateTime,
        end: LocalDateTime,
        status: String
    ): List<Appointment>
}
