package com.api.clinic_apointment.service

import com.api.clinic_apointment.entity.Service
import com.api.clinic_apointment.repository.ServiceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal

class ServiceCatalogServiceTest {

    private lateinit var serviceRepository: ServiceRepository
    private lateinit var serviceCatalogService: ServiceCatalogService

    @BeforeEach
    fun setUp() {
        serviceRepository = mock(ServiceRepository::class.java)
        serviceCatalogService = ServiceCatalogService(serviceRepository)
    }

    @Test
    fun `findAll maps service entities to response DTOs`() {
        val consultation = Service(
            id = 10L,
            name = "Consultation",
            price = BigDecimal("150.00"),
            durationMinutes = 30
        )
        val xray = Service(
            id = 20L,
            name = "X-Ray",
            price = BigDecimal("300.50"),
            durationMinutes = 20
        )

        `when`(serviceRepository.findAll()).thenReturn(listOf(consultation, xray))

        val responses = serviceCatalogService.findAll()

        assertEquals(2, responses.size)
        assertEquals(10L, responses[0].id)
        assertEquals("Consultation", responses[0].name)
        assertEquals(BigDecimal("150.00"), responses[0].price)
        assertEquals(30, responses[0].durationMinutes)
        assertEquals(20L, responses[1].id)
        assertEquals("X-Ray", responses[1].name)
        assertEquals(BigDecimal("300.50"), responses[1].price)
        assertEquals(20, responses[1].durationMinutes)
        verify(serviceRepository, times(1)).findAll()
    }

    @Test
    fun `findAll returns empty list when repository has no services`() {
        `when`(serviceRepository.findAll()).thenReturn(emptyList())

        val responses = serviceCatalogService.findAll()

        assertTrue(responses.isEmpty())
        verify(serviceRepository, times(1)).findAll()
    }
}
