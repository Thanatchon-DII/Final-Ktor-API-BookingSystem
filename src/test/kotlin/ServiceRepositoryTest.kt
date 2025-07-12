package com.example

import com.example.models.ServiceRequest
import com.example.repositories.ServiceRepository
import kotlin.test.*

class ServiceRepositoryTest {

    @BeforeTest
    fun setup() {
        // Reset repository to initial state before each test
        // สามารถเรียกใช้ reset() ได้โดยตรง
        ServiceRepository.reset()
    }

    @Test
    fun `getAll should return all services`() {
        val services = ServiceRepository.getAll()

        assertEquals(3, services.size)
        assertEquals("ตัดผมชาย", services[0].name)
        assertEquals("ตัดผมหญิง", services[1].name)
        assertEquals("ย้อมสีผม", services[2].name)
    }

    @Test
    fun `getById should return correct service when exists`() {
        val service = ServiceRepository.getById(1)

        assertNotNull(service)
        assertEquals(1, service.id)
        assertEquals("ตัดผมชาย", service.name)
        assertEquals(30, service.defaultDurationInMinutes)
    }

    @Test
    fun `getById should return null when service does not exist`() {
        val service = ServiceRepository.getById(999)
        assertNull(service)
    }

    @Test
    fun `add should create new service with incremented id`() {
        // ต้องใช้ ServiceRequest จาก models package
        val serviceRequest = ServiceRequest(
            name = "Test Service",
            description = "Test Description",
            defaultDurationInMinutes = 45
        )

        val newService = ServiceRepository.add(serviceRequest)

        assertEquals(4, newService.id)
        assertEquals("Test Service", newService.name)
        assertEquals("Test Description", newService.description)
        assertEquals(45, newService.defaultDurationInMinutes)

        // Verify it's added to the list
        val allServices = ServiceRepository.getAll()
        assertEquals(4, allServices.size)
        assertTrue(allServices.contains(newService))
    }

    @Test
    fun `multiple add operations should increment id correctly`() {
        val service1 = ServiceRepository.add(ServiceRequest("Service 1", "Desc 1", 30))
        val service2 = ServiceRepository.add(ServiceRequest("Service 2", "Desc 2", 60))

        assertEquals(4, service1.id)
        assertEquals(5, service2.id)
    }

    @Test
    fun `update should modify existing service`() {
        val updateRequest = ServiceRequest(
            name = "Updated Service",
            description = "Updated Description",
            defaultDurationInMinutes = 90
        )

        val updatedService = ServiceRepository.update(1, updateRequest)

        assertNotNull(updatedService)
        assertEquals(1, updatedService.id)
        assertEquals("Updated Service", updatedService.name)
        assertEquals("Updated Description", updatedService.description)
        assertEquals(90, updatedService.defaultDurationInMinutes)

        // Verify it's updated in the list
        val retrievedService = ServiceRepository.getById(1)
        assertEquals(updatedService, retrievedService)
    }

    @Test
    fun `update should return null when service does not exist`() {
        val updateRequest = ServiceRequest("Test", "Test", 30)
        val result = ServiceRepository.update(999, updateRequest)
        assertNull(result)
    }

    @Test
    fun `delete should remove existing service`() {
        val deleted = ServiceRepository.delete(1)

        assertTrue(deleted)
        assertNull(ServiceRepository.getById(1))
        assertEquals(2, ServiceRepository.getAll().size)
    }

    @Test
    fun `delete should return false when service does not exist`() {
        val deleted = ServiceRepository.delete(999)
        assertFalse(deleted)
        assertEquals(3, ServiceRepository.getAll().size)
    }
}