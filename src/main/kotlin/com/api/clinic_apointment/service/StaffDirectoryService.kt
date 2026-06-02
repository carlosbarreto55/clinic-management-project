package com.api.clinic_apointment.service

import com.api.clinic_apointment.dto.StaffResponse
import com.api.clinic_apointment.repository.StaffRepository
import org.springframework.stereotype.Service

@Service
class StaffDirectoryService(private val staffRepository: StaffRepository) {

    fun findAll(): List<StaffResponse> = staffRepository.findAll().map {
        StaffResponse(
            id = it.id,
            name = it.name
        )
    }
}
