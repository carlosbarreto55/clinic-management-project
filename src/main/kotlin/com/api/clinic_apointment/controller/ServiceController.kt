package com.api.clinic_apointment.controller

import com.api.clinic_apointment.dto.ServiceResponse
import com.api.clinic_apointment.service.ServiceCatalogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/services")
class ServiceController(private val serviceCatalogService: ServiceCatalogService) {

    @GetMapping
    fun getServices(): ResponseEntity<List<ServiceResponse>> {
        return ResponseEntity.ok(serviceCatalogService.findAll())
    }
}
