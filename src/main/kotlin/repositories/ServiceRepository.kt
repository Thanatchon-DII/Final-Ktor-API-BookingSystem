package com.example.repositories

import com.example.models.Service
import com.example.models.ServiceRequest

object ServiceRepository {
    // โค้ด ServiceRepository เดิม
    private val services = mutableListOf<Service>(
        Service(id = 1, name = "ตัดผมชาย", description = "บริการตัดผมสำหรับผู้ชาย", defaultDurationInMinutes = 30),
        Service(id = 2, name = "ตัดผมหญิง", description = "บริการตัดผมสำหรับผู้หญิง", defaultDurationInMinutes = 45),
        Service(id = 3, name = "ย้อมสีผม", description = "บริการย้อมสีผม", defaultDurationInMinutes = 120)
    )

    private var nextId = 4

    fun getAll(): List<Service> = services.toList()

    fun getById(id: Int): Service? = services.find { it.id == id }

    fun add(serviceRequest: ServiceRequest): Service {
        val newService = Service(
            id = nextId++,
            name = serviceRequest.name,
            description = serviceRequest.description,
            defaultDurationInMinutes = serviceRequest.defaultDurationInMinutes
        )
        services.add(newService)
        return newService
    }

    fun update(id: Int, serviceRequest: ServiceRequest): Service? {
        val index = services.indexOfFirst { it.id == id }
        return if (index != -1) {
            val updatedService = Service(
                id = id,
                name = serviceRequest.name,
                description = serviceRequest.description,
                defaultDurationInMinutes = serviceRequest.defaultDurationInMinutes
            )
            services[index] = updatedService
            updatedService
        } else {
            null
        }
    }

    fun delete(id: Int): Boolean {
        return services.removeIf { it.id == id }
    }

    fun reset() {
        services.clear()
        services.addAll(listOf(
            Service(id = 1, name = "ตัดผมชาย", description = "บริการตัดผมสำหรับผู้ชาย", defaultDurationInMinutes = 30),
            Service(id = 2, name = "ตัดผมหญิง", description = "บริการตัดผมสำหรับผู้หญิง", defaultDurationInMinutes = 45),
            Service(id = 3, name = "ย้อมสีผม", description = "บริการย้อมสีผม", defaultDurationInMinutes = 120)
        ))
        nextId = 4
    }
}