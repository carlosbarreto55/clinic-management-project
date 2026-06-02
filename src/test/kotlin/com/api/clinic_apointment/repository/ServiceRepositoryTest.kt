package com.api.clinic_apointment.repository

import com.api.clinic_apointment.entity.Service
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.math.BigDecimal

@DataJpaTest
class ServiceRepositoryTest @Autowired constructor(
    private val serviceRepository: ServiceRepository
) {

    @Test
    fun `save service should persist and return service with id`() {
        val service = Service(
            name = "Consultation",
            price = BigDecimal("150.00"),
            durationMinutes = 30
        )
        val saved = serviceRepository.save(service)

        assertNotNull(saved.id)
        assertEquals("Consultation", saved.name)
        assertEquals(BigDecimal("150.00"), saved.price)
        assertEquals(30, saved.durationMinutes)
    }

    @Test
    fun `save service should handle zero price`() {
        val service = Service(
            name = "Free Checkup",
            price = BigDecimal.ZERO,
            durationMinutes = 15
        )
        val saved = serviceRepository.save(service)

        assertNotNull(saved.id)
        assertEquals(BigDecimal.ZERO, saved.price)
    }

    @Test
    fun `save service should handle high precision price`() {
        val service = Service(
            name = "Special Surgery",
            price = BigDecimal("12345.67"),
            durationMinutes = 180
        )
        val saved = serviceRepository.save(service)

        assertEquals(BigDecimal("12345.67"), saved.price)
    }

    @Test
    fun `findById should return service when id exists`() {
        val service = Service(
            name = "Dental Cleaning",
            price = BigDecimal("80.00"),
            durationMinutes = 45
        )
        val saved = serviceRepository.save(service)

        val found = serviceRepository.findById(saved.id)

        assertTrue(found.isPresent)
        assertEquals("Dental Cleaning", found.get().name)
        assertEquals(BigDecimal("80.00"), found.get().price)
    }

    @Test
    fun `findById should return empty optional when id does not exist`() {
        val found = serviceRepository.findById(999L)

        assertFalse(found.isPresent)
    }

    @Test
    fun `findAll should return all services`() {
        serviceRepository.save(Service(name = "Exam", price = BigDecimal("200.00"), durationMinutes = 60))
        serviceRepository.save(Service(name = "X-Ray", price = BigDecimal("350.00"), durationMinutes = 20))

        val all = serviceRepository.findAll()

        assertEquals(2, all.size)
        val names = all.map { it.name }.toSet()
        assertTrue(names.containsAll(listOf("Exam", "X-Ray")))
    }

    @Test
    fun `findAll should return empty list when no services exist`() {
        val all = serviceRepository.findAll()

        assertTrue(all.isEmpty())
    }
}
