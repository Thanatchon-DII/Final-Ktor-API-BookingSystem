package com.example

import com.example.models.AppointmentRequest
import com.example.repositories.AppointmentRepository
import com.example.repositories.ServiceRepository
import kotlin.test.*

class AppointmentRepositoryTest {

    @BeforeTest
    fun setup() {
        // Reset repositories to initial state
        // ไฟล์ test สามารถเรียกใช้ reset() ได้โดยตรง เพราะเรา import repositories มาแล้ว
        ServiceRepository.reset()
        AppointmentRepository.reset()
    }

    @Test
    fun `getAll should return empty list initially`() {
        val appointments = AppointmentRepository.getAll()
        assertTrue(appointments.isEmpty())
    }

    @Test
    fun `getById should return null when no appointments exist`() {
        val appointment = AppointmentRepository.getById(1)
        assertNull(appointment)
    }

    @Test
    fun `add should create new appointment when valid`() {
        // ต้องใช้ AppointmentRequest จาก models package
        val request = AppointmentRequest(
            clientName = "Test Client",
            clientEmail = "test@email.com",
            appointmentTime = "2025-07-15T10:00:00",
            serviceId = 1
        )

        val result = AppointmentRepository.add(request)

        assertTrue(result.isSuccess)
        val appointment = result.getOrNull()
        assertNotNull(appointment)
        assertEquals(1, appointment.id)
        assertEquals("Test Client", appointment.clientName)
        assertEquals("test@email.com", appointment.clientEmail)
        assertEquals("2025-07-15T10:00:00", appointment.appointmentTime)
        assertEquals(1, appointment.serviceId)
    }

    @Test
    fun `add should fail when service does not exist`() {
        val request = AppointmentRequest(
            clientName = "Test Client",
            clientEmail = "test@email.com",
            appointmentTime = "2025-07-15T10:00:00",
            serviceId = 999
        )

        val result = AppointmentRepository.add(request)

        assertTrue(result.isFailure)
        assertEquals("Service not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `add should fail with invalid date format`() {
        val request = AppointmentRequest(
            clientName = "Test Client",
            clientEmail = "test@email.com",
            appointmentTime = "2025-07-15 10:00:00", // Wrong format
            serviceId = 1
        )

        val result = AppointmentRepository.add(request)

        assertTrue(result.isFailure)
        assertEquals("Invalid date time format. Use yyyy-MM-ddTHH:mm:ss", result.exceptionOrNull()?.message)
    }

    @Test
    fun `add should prevent double booking - exact same time`() {
        // Create first appointment
        val request1 = AppointmentRequest(
            clientName = "Client 1",
            clientEmail = "client1@email.com",
            appointmentTime = "2025-07-15T10:00:00",
            serviceId = 1 // 30 minutes duration
        )
        AppointmentRepository.add(request1)

        // Try to create second appointment at same time
        val request2 = AppointmentRequest(
            clientName = "Client 2",
            clientEmail = "client2@email.com",
            appointmentTime = "2025-07-15T10:00:00",
            serviceId = 1
        )

        val result = AppointmentRepository.add(request2)

        assertTrue(result.isFailure)
        assertEquals("Time slot is already booked. Please choose a different time.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `add should prevent double booking - overlapping times`() {
        // Create first appointment (10:00-10:30)
        val request1 = AppointmentRequest(
            clientName = "Client 1",
            clientEmail = "client1@email.com",
            appointmentTime = "2025-07-15T10:00:00",
            serviceId = 1 // 30 minutes
        )
        AppointmentRepository.add(request1)

        // Try to create overlapping appointment (10:15-10:45)
        val request2 = AppointmentRequest(
            clientName = "Client 2",
            clientEmail = "client2@email.com",
            appointmentTime = "2025-07-15T10:15:00",
            serviceId = 1 // 30 minutes
        )

        val result = AppointmentRepository.add(request2)

        assertTrue(result.isFailure)
        assertEquals("Time slot is already booked. Please choose a different time.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `add should allow non-overlapping appointments`() {
        // Create first appointment (10:00-10:30)
        val request1 = AppointmentRequest(
            clientName = "Client 1",
            clientEmail = "client1@email.com",
            appointmentTime = "2025-07-15T10:00:00",
            serviceId = 1 // 30 minutes
        )
        val result1 = AppointmentRepository.add(request1)
        assertTrue(result1.isSuccess)

        // Create second appointment (10:30-11:00) - should be allowed
        val request2 = AppointmentRequest(
            clientName = "Client 2",
            clientEmail = "client2@email.com",
            appointmentTime = "2025-07-15T10:30:00",
            serviceId = 1 // 30 minutes
        )
        val result2 = AppointmentRepository.add(request2)
        assertTrue(result2.isSuccess)

        assertEquals(2, AppointmentRepository.getAll().size)
    }

    @Test
    fun `add should handle different service durations correctly`() {
        // Create appointment with service 3 (120 minutes: 10:00-12:00)
        val request1 = AppointmentRequest(
            clientName = "Client 1",
            clientEmail = "client1@email.com",
            appointmentTime = "2025-07-15T10:00:00",
            serviceId = 3 // 120 minutes (ย้อมสีผม)
        )
        val result1 = AppointmentRepository.add(request1)
        assertTrue(result1.isSuccess)

        // Try to book at 11:00 (should conflict)
        val request2 = AppointmentRequest(
            clientName = "Client 2",
            clientEmail = "client2@email.com",
            appointmentTime = "2025-07-15T11:00:00",
            serviceId = 1 // 30 minutes
        )
        val result2 = AppointmentRepository.add(request2)
        assertTrue(result2.isFailure)

        // Book at 12:00 (should be allowed)
        val request3 = AppointmentRequest(
            clientName = "Client 3",
            clientEmail = "client3@email.com",
            appointmentTime = "2025-07-15T12:00:00",
            serviceId = 1 // 30 minutes
        )
        val result3 = AppointmentRepository.add(request3)
        assertTrue(result3.isSuccess)
    }

    @Test
    fun `update should modify existing appointment`() {
        // Create initial appointment
        val createRequest = AppointmentRequest(
            clientName = "Original Client",
            clientEmail = "original@email.com",
            appointmentTime = "2025-07-15T10:00:00",
            serviceId = 1
        )
        val createResult = AppointmentRepository.add(createRequest)
        assertTrue(createResult.isSuccess)
        val appointmentId = createResult.getOrNull()!!.id

        // Update appointment
        val updateRequest = AppointmentRequest(
            clientName = "Updated Client",
            clientEmail = "updated@email.com",
            appointmentTime = "2025-07-15T11:00:00",
            serviceId = 2
        )

        val updateResult = AppointmentRepository.update(appointmentId, updateRequest)

        assertTrue(updateResult.isSuccess)
        val updatedAppointment = updateResult.getOrNull()
        assertNotNull(updatedAppointment)
        assertEquals("Updated Client", updatedAppointment.clientName)
        assertEquals("updated@email.com", updatedAppointment.clientEmail)
        assertEquals("2025-07-15T11:00:00", updatedAppointment.appointmentTime)
        assertEquals(2, updatedAppointment.serviceId)
    }

    @Test
    fun `update should return null when appointment does not exist`() {
        val updateRequest = AppointmentRequest(
            clientName = "Test",
            clientEmail = "test@email.com",
            appointmentTime = "2025-07-15T10:00:00",
            serviceId = 1
        )

        val result = AppointmentRepository.update(999, updateRequest)

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `update should prevent double booking with other appointments`() {
        // Create two appointments
        val request1 = AppointmentRequest("Client 1", "client1@email.com", "2025-07-15T10:00:00", 1)
        val request2 = AppointmentRequest("Client 2", "client2@email.com", "2025-07-15T11:00:00", 1)

        val result1 = AppointmentRepository.add(request1)
        val result2 = AppointmentRepository.add(request2)
        val appointment1Id = result1.getOrNull()!!.id

        // Try to update appointment1 to conflict with appointment2
        val updateRequest = AppointmentRequest("Updated Client 1", "updated1@email.com", "2025-07-15T11:00:00", 1)
        val updateResult = AppointmentRepository.update(appointment1Id, updateRequest)

        assertTrue(updateResult.isFailure)
        assertEquals("Time slot is already booked. Please choose a different time.", updateResult.exceptionOrNull()?.message)
    }

    @Test
    fun `update should allow updating appointment to same time (self-update)`() {
        // Create appointment
        val createRequest = AppointmentRequest("Client", "client@email.com", "2025-07-15T10:00:00", 1)
        val createResult = AppointmentRepository.add(createRequest)
        val appointmentId = createResult.getOrNull()!!.id

        // Update with same time but different details
        val updateRequest = AppointmentRequest("Updated Client", "updated@email.com", "2025-07-15T10:00:00", 1)
        val updateResult = AppointmentRepository.update(appointmentId, updateRequest)

        assertTrue(updateResult.isSuccess)
        val updatedAppointment = updateResult.getOrNull()
        assertNotNull(updatedAppointment)
        assertEquals("Updated Client", updatedAppointment.clientName)
    }

    @Test
    fun `delete should remove existing appointment`() {
        // Create appointment
        val request = AppointmentRequest("Client", "client@email.com", "2025-07-15T10:00:00", 1)
        val createResult = AppointmentRepository.add(request)
        val appointmentId = createResult.getOrNull()!!.id

        // Delete appointment
        val deleted = AppointmentRepository.delete(appointmentId)

        assertTrue(deleted)
        assertNull(AppointmentRepository.getById(appointmentId))
        assertTrue(AppointmentRepository.getAll().isEmpty())
    }

    @Test
    fun `delete should return false when appointment does not exist`() {
        val deleted = AppointmentRepository.delete(999)
        assertFalse(deleted)
    }

    @Test
    fun `getById should return correct appointment after creation`() {
        val request = AppointmentRequest("Client", "client@email.com", "2025-07-15T10:00:00", 1)
        val createResult = AppointmentRepository.add(request)
        val appointmentId = createResult.getOrNull()!!.id

        val retrieved = AppointmentRepository.getById(appointmentId)

        assertNotNull(retrieved)
        assertEquals(appointmentId, retrieved.id)
        assertEquals("Client", retrieved.clientName)
    }

    @Test
    fun `multiple appointments should increment id correctly`() {
        val request1 = AppointmentRequest("Client 1", "client1@email.com", "2025-07-15T10:00:00", 1)
        val request2 = AppointmentRequest("Client 2", "client2@email.com", "2025-07-15T11:00:00", 1)

        val result1 = AppointmentRepository.add(request1)
        val result2 = AppointmentRepository.add(request2)

        assertEquals(1, result1.getOrNull()!!.id)
        assertEquals(2, result2.getOrNull()!!.id)
    }
}