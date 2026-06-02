package com.api.clinic_apointment.service

import com.api.clinic_apointment.dto.StaffResponse
import com.api.clinic_apointment.entity.Staff
import com.api.clinic_apointment.repository.StaffRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class StaffDirectoryServiceTest {

    private lateinit var staffRepository: StaffRepository
    private lateinit var staffDirectoryService: StaffDirectoryService

    @BeforeEach
    fun setUp() {
        staffRepository = mock(StaffRepository::class.java)
        staffDirectoryService = StaffDirectoryService(staffRepository)
    }

    @Test
    fun `findAll maps staff entities to response DTOs`() {
        val drSmith = Staff(id = 10L, name = "Dr. Smith", userId = 100L)
        val drJones = Staff(id = 20L, name = "Dr. Jones", userId = 200L)

        `when`(staffRepository.findAll()).thenReturn(listOf(drSmith, drJones))

        val responses = staffDirectoryService.findAll()

        assertEquals(2, responses.size)
        assertEquals(10L, responses[0].id)
        assertEquals("Dr. Smith", responses[0].name)
        assertEquals(20L, responses[1].id)
        assertEquals("Dr. Jones", responses[1].name)
        verify(staffRepository, times(1)).findAll()
    }

    @Test
    fun `staff response DTO does not expose userId`() {
        val dtoFieldNames = StaffResponse::class.java.declaredFields.map { it.name }

        assertFalse(dtoFieldNames.contains("userId"))
    }

    @Test
    fun `findAll returns empty list when repository has no staff`() {
        `when`(staffRepository.findAll()).thenReturn(emptyList())

        val responses = staffDirectoryService.findAll()

        assertTrue(responses.isEmpty())
        verify(staffRepository, times(1)).findAll()
    }
}
