package com.api.clinic_apointment.repository

import com.api.clinic_apointment.entity.Service
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ServiceRepository : JpaRepository<Service, Long>
