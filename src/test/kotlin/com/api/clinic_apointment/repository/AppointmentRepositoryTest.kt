package com.api.clinic_apointment.repository

import com.api.clinic_apointment.entity.Appointment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.LocalDateTime

@DataJpaTest
class AppointmentRepositoryTest @Autowired constructor(
    private val appointmentRepository: AppointmentRepository
) {

    @Test
    fun `save and findById should persist and retrieve appointment`() {
        val now = LocalDateTime.now()
        val appointment = Appointment(
            clientName = "John Doe",
            startTime = now.plusHours(1),
            endTime = now.plusHours(2),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        val saved = appointmentRepository.save(appointment)

        val found = appointmentRepository.findById(saved.id)

        assertTrue(found.isPresent)
        assertEquals("John Doe", found.get().clientName)
        assertEquals("ACTIVE", found.get().status)
        assertEquals("APPOINTMENT", found.get().type)
        assertEquals(1L, found.get().serviceId)
        assertEquals(1L, found.get().staffId)
    }

    @Test
    fun `save should persist appointment with null clientName for LOCK type`() {
        val now = LocalDateTime.now()
        val appointment = Appointment(
            clientName = null,
            startTime = now.plusHours(1),
            endTime = now.plusHours(2),
            status = "ACTIVE",
            type = "LOCK",
            serviceId = 0L,
            staffId = 2L,
            createdAt = now,
            updatedAt = now
        )
        val saved = appointmentRepository.save(appointment)

        assertNotNull(saved.id)
        assertEquals(null, saved.clientName)
        assertEquals(0L, saved.serviceId)
        assertEquals("LOCK", saved.type)
    }

    @Test
    fun `countOverlapping should return 0 when no overlap exists`() {
        val now = LocalDateTime.now()
        val existing = Appointment(
            clientName = "Jane Doe",
            startTime = now.plusHours(1),
            endTime = now.plusHours(2),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        appointmentRepository.save(existing)

        val count = appointmentRepository.countOverlapping(1L, now.plusHours(2), now.plusHours(3))

        assertEquals(0, count)
    }

    @Test
    fun `countOverlapping should return 1 when full overlap exists`() {
        val now = LocalDateTime.now()
        val existing = Appointment(
            clientName = "Jane Doe",
            startTime = now.plusHours(1),
            endTime = now.plusHours(3),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        appointmentRepository.save(existing)

        val count = appointmentRepository.countOverlapping(
            1L,
            now.plusHours(1).plusMinutes(30),
            now.plusHours(2).plusMinutes(30)
        )

        assertEquals(1, count)
    }

    @Test
    fun `countOverlapping should return 1 when partial overlap exists`() {
        val now = LocalDateTime.now()
        val existing = Appointment(
            clientName = "Jane Doe",
            startTime = now.plusHours(1),
            endTime = now.plusHours(2),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        appointmentRepository.save(existing)

        val count = appointmentRepository.countOverlapping(
            1L,
            now.plusHours(1).plusMinutes(30),
            now.plusHours(2).plusMinutes(30)
        )

        assertEquals(1, count)
    }

    @Test
    fun `countOverlapping should return 1 when exact same start and end times`() {
        val now = LocalDateTime.now()
        val existing = Appointment(
            clientName = "Jane Doe",
            startTime = now.plusHours(1),
            endTime = now.plusHours(2),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        appointmentRepository.save(existing)

        val count = appointmentRepository.countOverlapping(1L, now.plusHours(1), now.plusHours(2))

        assertEquals(1, count)
    }

    @Test
    fun `countOverlapping should return 0 when only cancelled appointments exist`() {
        val now = LocalDateTime.now()
        val existing = Appointment(
            clientName = "Cancelled Patient",
            startTime = now.plusHours(1),
            endTime = now.plusHours(2),
            status = "CANCELLED",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        appointmentRepository.save(existing)

        val count = appointmentRepository.countOverlapping(1L, now.plusHours(1), now.plusHours(2))

        assertEquals(0, count)
    }

    @Test
    fun `countOverlapping should return 0 when overlap is on different staff`() {
        val now = LocalDateTime.now()
        val existing = Appointment(
            clientName = "Patient on Staff 1",
            startTime = now.plusHours(1),
            endTime = now.plusHours(2),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        appointmentRepository.save(existing)

        val count = appointmentRepository.countOverlapping(2L, now.plusHours(1), now.plusHours(2))

        assertEquals(0, count)
    }

    @Test
    fun `countOverlapping should return 0 when multiple cancelled and one active exists but on different staff`() {
        val now = LocalDateTime.now()
        appointmentRepository.save(
            Appointment(
                clientName = null,
                startTime = now.plusHours(1),
                endTime = now.plusHours(2),
                status = "CANCELLED",
                type = "LOCK",
                serviceId = 0L,
                staffId = 1L,
                createdAt = now,
                updatedAt = now
            )
        )
        appointmentRepository.save(
            Appointment(
                clientName = null,
                startTime = now.plusHours(1),
                endTime = now.plusHours(2),
                status = "CANCELLED",
                type = "LOCK",
                serviceId = 0L,
                staffId = 1L,
                createdAt = now,
                updatedAt = now
            )
        )

        val count = appointmentRepository.countOverlapping(1L, now.plusHours(1), now.plusHours(2))

        assertEquals(0, count)
    }

    @Test
    fun `findByStartTimeBetweenAndStatusNot should return appointments in range excluding cancelled`() {
        val now = LocalDateTime.now()
        val active1 = Appointment(
            clientName = "Patient A",
            startTime = now.plusHours(1),
            endTime = now.plusHours(2),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        val cancelled = Appointment(
            clientName = "Patient B",
            startTime = now.plusHours(1).plusMinutes(30),
            endTime = now.plusHours(2).plusMinutes(30),
            status = "CANCELLED",
            type = "APPOINTMENT",
            serviceId = 2L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        val active2 = Appointment(
            clientName = "Patient C",
            startTime = now.plusHours(3),
            endTime = now.plusHours(4),
            status = "ACTIVE",
            type = "LOCK",
            serviceId = 0L,
            staffId = 2L,
            createdAt = now,
            updatedAt = now
        )
        val outOfRange = Appointment(
            clientName = "Patient D",
            startTime = now.plusHours(5),
            endTime = now.plusHours(6),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        appointmentRepository.saveAll(listOf(active1, cancelled, active2, outOfRange))

        val results = appointmentRepository.findByStartTimeBetweenAndStatusNot(
            now.plusHours(0), now.plusHours(4), "CANCELLED"
        )

        assertEquals(2, results.size)
        assertTrue(results.all { it.status != "CANCELLED" })
        val names = results.map { it.clientName }.toSet()
        assertTrue(names.contains("Patient A"))
        assertTrue(names.contains("Patient C"))
        assertFalse(names.contains("Patient B"))
        assertFalse(names.contains("Patient D"))
    }

    @Test
    fun `findByStartTimeBetweenAndStatusNot should return empty list when no appointments match range`() {
        val now = LocalDateTime.now()
        val outOfRange = Appointment(
            clientName = "Late Patient",
            startTime = now.plusHours(10),
            endTime = now.plusHours(11),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        appointmentRepository.save(outOfRange)

        val results = appointmentRepository.findByStartTimeBetweenAndStatusNot(
            now.plusHours(0), now.plusHours(5), "CANCELLED"
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun `findByStartTimeBetweenAndStatusNot should include appointments at boundary times`() {
        val now = LocalDateTime.now()
        val atStart = Appointment(
            clientName = "Boundary Start",
            startTime = now.plusHours(0),
            endTime = now.plusHours(1),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        val atEnd = Appointment(
            clientName = "Boundary End",
            startTime = now.plusHours(4),
            endTime = now.plusHours(5),
            status = "ACTIVE",
            type = "APPOINTMENT",
            serviceId = 1L,
            staffId = 1L,
            createdAt = now,
            updatedAt = now
        )
        appointmentRepository.saveAll(listOf(atStart, atEnd))

        val results = appointmentRepository.findByStartTimeBetweenAndStatusNot(
            now.plusHours(0), now.plusHours(4), "CANCELLED"
        )

        assertEquals(2, results.size)
    }
}
