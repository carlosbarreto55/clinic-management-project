package com.api.clinic_apointment.controller

import com.api.clinic_apointment.dto.StaffResponse
import com.api.clinic_apointment.service.StaffDirectoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/staff")
class StaffController(private val staffDirectoryService: StaffDirectoryService) {

    @GetMapping
    fun getStaff(): ResponseEntity<List<StaffResponse>> {
        return ResponseEntity.ok(staffDirectoryService.findAll())
    }
}
