package com.api.clinic_apointment.service

import com.api.clinic_apointment.dto.ServiceResponse
import com.api.clinic_apointment.repository.ServiceRepository
import org.springframework.stereotype.Service

@Service
class ServiceCatalogService(private val serviceRepository: ServiceRepository) {

    fun findAll(): List<ServiceResponse> = serviceRepository.findAll().map {
        ServiceResponse(
            id = it.id,
            name = it.name,
            price = it.price,
            durationMinutes = it.durationMinutes
        )
    }
}
