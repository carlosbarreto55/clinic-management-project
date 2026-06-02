package com.api.clinic_apointment.event

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class AppointmentEventListener {
    @Async
    @EventListener
    fun onAppointmentEvent(event: AppointmentEvent) {
        // Phase 5 will fan this event out to active SSE connections.
    }
}
