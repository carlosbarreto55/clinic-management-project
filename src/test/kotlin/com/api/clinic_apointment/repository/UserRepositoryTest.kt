package com.api.clinic_apointment.repository

import com.api.clinic_apointment.entity.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
class UserRepositoryTest @Autowired constructor(
    private val userRepository: UserRepository,
    private val entityManager: TestEntityManager
) {

    @Test
    fun `save user should persist and return user with id`() {
        val user = User(
            email = "test@example.com",
            passwordHash = "hashed_password_123",
            role = "RECEPTIONIST"
        )
        val saved = userRepository.save(user)

        assertNotNull(saved.id)
        assertEquals("test@example.com", saved.email)
        assertEquals("hashed_password_123", saved.passwordHash)
        assertEquals("RECEPTIONIST", saved.role)
    }

    @Test
    fun `findByEmail should return user when email exists`() {
        val user = User(
            email = "jane@clinic.com",
            passwordHash = "hash_abc",
            role = "STAFF"
        )
        entityManager.persist(user)
        entityManager.flush()

        val found = userRepository.findByEmail("jane@clinic.com")

        assertNotNull(found)
        assertEquals("jane@clinic.com", found!!.email)
        assertEquals("STAFF", found.role)
    }

    @Test
    fun `findByEmail should return null when email does not exist`() {
        val found = userRepository.findByEmail("nonexistent@clinic.com")

        assertNull(found)
    }

    @Test
    fun `findByEmail should be case sensitive`() {
        val user = User(
            email = "CaseSensitive@clinic.com",
            passwordHash = "hash_def",
            role = "ADMIN"
        )
        entityManager.persist(user)
        entityManager.flush()

        val foundLowercase = userRepository.findByEmail("casesensitive@clinic.com")
        assertNull(foundLowercase)

        val foundExact = userRepository.findByEmail("CaseSensitive@clinic.com")
        assertNotNull(foundExact)
        assertEquals("CaseSensitive@clinic.com", foundExact!!.email)
    }

    @Test
    fun `save user should generate unique id for each user`() {
        val user1 = User(email = "user1@clinic.com", passwordHash = "hash1", role = "STAFF")
        val user2 = User(email = "user2@clinic.com", passwordHash = "hash2", role = "RECEPTIONIST")

        val saved1 = userRepository.save(user1)
        val saved2 = userRepository.save(user2)

        assertNotNull(saved1.id)
        assertNotNull(saved2.id)
        assert(saved1.id != saved2.id)
    }
}
