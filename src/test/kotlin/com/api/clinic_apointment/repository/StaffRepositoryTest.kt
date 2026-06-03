package com.api.clinic_apointment.repository

import com.api.clinic_apointment.entity.Staff
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class StaffRepositoryTest @Autowired constructor(
    private val staffRepository: StaffRepository
) {

    @Test
    fun `save staff should persist and return staff with id`() {
        val staff = Staff(name = "Dr. Smith", userId = 1L)
        val saved = staffRepository.save(staff)

        assertNotNull(saved.id)
        assertEquals("Dr. Smith", saved.name)
        assertEquals(1L, saved.userId)
    }

    @Test
    fun `findById should return staff when id exists`() {
        val staff = Staff(name = "Dr. Jones", userId = 2L)
        val saved = staffRepository.save(staff)

        val found = staffRepository.findById(saved.id)

        assertTrue(found.isPresent)
        assertEquals("Dr. Jones", found.get().name)
        assertEquals(2L, found.get().userId)
    }

    @Test
    fun `findById should return empty optional when id does not exist`() {
        val found = staffRepository.findById(999L)

        assertFalse(found.isPresent)
    }

    @Test
    fun `findByUserId should return staff mapped to authenticated user`() {
        staffRepository.save(Staff(name = "Dr. Owner", userId = 10L))
        staffRepository.save(Staff(name = "Dr. Other", userId = 11L))

        val found = staffRepository.findByUserId(10L)

        assertTrue(found.isPresent)
        assertEquals("Dr. Owner", found.get().name)
        assertEquals(10L, found.get().userId)
    }

    @Test
    fun `findByUserId should return empty optional when authenticated user is not staff`() {
        staffRepository.save(Staff(name = "Dr. Owner", userId = 10L))

        val found = staffRepository.findByUserId(999L)

        assertFalse(found.isPresent)
    }

    @Test
    fun `findAll should return all staff`() {
        staffRepository.save(Staff(name = "Dr. Lee", userId = 3L))
        staffRepository.save(Staff(name = "Dr. Kim", userId = 4L))
        staffRepository.save(Staff(name = "Dr. Park", userId = 5L))

        val all = staffRepository.findAll()

        assertEquals(3, all.size)
        val names = all.map { it.name }.toSet()
        assertTrue(names.containsAll(listOf("Dr. Lee", "Dr. Kim", "Dr. Park")))
    }

    @Test
    fun `findAll should return empty list when no staff exists`() {
        val all = staffRepository.findAll()

        assertTrue(all.isEmpty())
    }

    @Test
    fun `delete staff should remove staff from repository`() {
        val staff = Staff(name = "Dr. ToDelete", userId = 6L)
        val saved = staffRepository.save(staff)
        assertEquals(1, staffRepository.findAll().size)

        staffRepository.delete(saved)

        val all = staffRepository.findAll()
        assertTrue(all.isEmpty())
    }
}
