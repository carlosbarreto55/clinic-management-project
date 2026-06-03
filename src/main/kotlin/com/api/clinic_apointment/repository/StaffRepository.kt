package com.api.clinic_apointment.repository

import com.api.clinic_apointment.entity.Staff
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface StaffRepository : JpaRepository<Staff, Long> {
    fun findByUserId(userId: Long): Optional<Staff>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Staff s WHERE s.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Optional<Staff>
}
