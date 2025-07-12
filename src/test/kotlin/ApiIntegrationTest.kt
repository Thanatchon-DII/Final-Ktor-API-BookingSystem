package com.example

import com.example.models.AppointmentRequest
import com.example.models.ServiceRequest
import com.example.repositories.AppointmentRepository
import com.example.repositories.ServiceRepository
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

// โค้ด Test ส่วนที่เหลือยังคงเดิม
class ApiIntegrationTest {

    private fun ApplicationTestBuilder.setupApp() {
        application {
            module()
        }
    }

    // @BeforeTest ยังคงเรียก ServiceRepository.reset() และ AppointmentRepository.reset() ได้เหมือนเดิม
    @BeforeTest
    fun setup() {
        ServiceRepository.reset()
        AppointmentRepository.reset()
    }

    // Services API Tests
    @Test
    fun `GET services should return all services`() = testApplication {
        setupApp()

        val response = client.get("/services")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("ตัดผมชาย"))
        assertTrue(responseBody.contains("ตัดผมหญิง"))
        assertTrue(responseBody.contains("ย้อมสีผม"))
    }

    @Test
    fun `GET services by valid id should return service`() = testApplication {
        setupApp()

        val response = client.get("/services/1")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("ตัดผมชาย"))
        assertTrue(responseBody.contains("\"id\":1"))
    }

    @Test
    fun `GET services by invalid id should return 404`() = testApplication {
        setupApp()

        val response = client.get("/services/999")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Service not found"))
    }

    @Test
    fun `GET services by non-numeric id should return 400`() = testApplication {
        setupApp()

        val response = client.get("/services/abc")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Invalid service ID"))
    }

    @Test
    fun `POST services with valid data should create service`() = testApplication {
        setupApp()

        val serviceRequest = ServiceRequest("Test Service", "Test Description", 60)
        val requestBody = Json.encodeToString(serviceRequest)

        val response = client.post("/services") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Test Service"))
        assertTrue(responseBody.contains("\"id\":4"))
    }

    @Test
    fun `POST services with empty name should return 400`() = testApplication {
        setupApp()

        val serviceRequest = ServiceRequest("", "Test Description", 60)
        val requestBody = Json.encodeToString(serviceRequest)

        val response = client.post("/services") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Service name cannot be empty"))
    }

    @Test
    fun `POST services with invalid duration should return 400`() = testApplication {
        setupApp()

        val serviceRequest = ServiceRequest("Test Service", "Test Description", 0)
        val requestBody = Json.encodeToString(serviceRequest)

        val response = client.post("/services") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Duration must be greater than 0"))
    }

    @Test
    fun `POST services with invalid JSON should return 400`() = testApplication {
        setupApp()

        val response = client.post("/services") {
            contentType(ContentType.Application.Json)
            setBody("invalid json")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Invalid request body"))
    }

    @Test
    fun `PUT services with valid data should update service`() = testApplication {
        setupApp()

        val serviceRequest = ServiceRequest("Updated Service", "Updated Description", 90)
        val requestBody = Json.encodeToString(serviceRequest)

        val response = client.put("/services/1") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Updated Service"))
        assertTrue(responseBody.contains("\"id\":1"))
    }

    @Test
    fun `PUT services with non-existing id should return 404`() = testApplication {
        setupApp()

        val serviceRequest = ServiceRequest("Updated Service", "Updated Description", 90)
        val requestBody = Json.encodeToString(serviceRequest)

        val response = client.put("/services/999") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Service not found"))
    }

    @Test
    fun `DELETE services should remove service`() = testApplication {
        setupApp()

        val response = client.delete("/services/1")

        assertEquals(HttpStatusCode.NoContent, response.status)

        // Verify it's deleted
        val getResponse = client.get("/services/1")
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }

    @Test
    fun `DELETE non-existing service should return 404`() = testApplication {
        setupApp()

        val response = client.delete("/services/999")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Service not found"))
    }

    // Appointments API Tests
    @Test
    fun `GET appointments should return empty array initially`() = testApplication {
        setupApp()

        val response = client.get("/appointments")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertEquals("[]", responseBody)
    }

    @Test
    fun `POST appointments with valid data should create appointment`() = testApplication {
        setupApp()

        val appointmentRequest = AppointmentRequest(
            "Test Client",
            "test@email.com",
            "2025-07-15T10:00:00",
            1
        )
        val requestBody = Json.encodeToString(appointmentRequest)

        val response = client.post("/appointments") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Test Client"))
        assertTrue(responseBody.contains("\"id\":1"))
    }

    @Test
    fun `POST appointments with conflicting time should return 409`() = testApplication {
        setupApp()

        // Create first appointment
        val appointment1 = AppointmentRequest("Client 1", "client1@email.com", "2025-07-15T10:00:00", 1)
        val requestBody1 = Json.encodeToString(appointment1)

        client.post("/appointments") {
            contentType(ContentType.Application.Json)
            setBody(requestBody1)
        }

        // Try to create conflicting appointment
        val appointment2 = AppointmentRequest("Client 2", "client2@email.com", "2025-07-15T10:15:00", 1)
        val requestBody2 = Json.encodeToString(appointment2)

        val response = client.post("/appointments") {
            contentType(ContentType.Application.Json)
            setBody(requestBody2)
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Time slot is already booked"))
    }

    @Test
    fun `POST appointments with invalid service should return 400`() = testApplication {
        setupApp()

        val appointmentRequest = AppointmentRequest(
            "Test Client",
            "test@email.com",
            "2025-07-15T10:00:00",
            999
        )
        val requestBody = Json.encodeToString(appointmentRequest)

        val response = client.post("/appointments") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Service not found"))
    }

    @Test
    fun `POST appointments with invalid date format should return 400`() = testApplication {
        setupApp()

        val appointmentRequest = AppointmentRequest(
            "Test Client",
            "test@email.com",
            "2025-07-15 10:00:00", // Wrong format
            1
        )
        val requestBody = Json.encodeToString(appointmentRequest)

        val response = client.post("/appointments") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Invalid date time format"))
    }

    @Test
    fun `POST appointments with empty client name should return 400`() = testApplication {
        setupApp()

        val appointmentRequest = AppointmentRequest(
            "",
            "test@email.com",
            "2025-07-15T10:00:00",
            1
        )
        val requestBody = Json.encodeToString(appointmentRequest)

        val response = client.post("/appointments") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Client name cannot be empty"))
    }

    @Test
    fun `GET appointments by id should return appointment`() = testApplication {
        setupApp()

        // Create appointment first
        val appointmentRequest = AppointmentRequest("Test Client", "test@email.com", "2025-07-15T10:00:00", 1)
        val requestBody = Json.encodeToString(appointmentRequest)

        client.post("/appointments") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        // Get appointment by id
        val response = client.get("/appointments/1")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Test Client"))
        assertTrue(responseBody.contains("\"id\":1"))
    }

    @Test
    fun `GET appointments by invalid id should return 404`() = testApplication {
        setupApp()

        val response = client.get("/appointments/999")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Appointment not found"))
    }

    @Test
    fun `PUT appointments should update appointment`() = testApplication {
        setupApp()

        // Create appointment first
        val createRequest = AppointmentRequest("Original Client", "original@email.com", "2025-07-15T10:00:00", 1)
        val createBody = Json.encodeToString(createRequest)

        client.post("/appointments") {
            contentType(ContentType.Application.Json)
            setBody(createBody)
        }

        // Update appointment
        val updateRequest = AppointmentRequest("Updated Client", "updated@email.com", "2025-07-15T11:00:00", 2)
        val updateBody = Json.encodeToString(updateRequest)

        val response = client.put("/appointments/1") {
            contentType(ContentType.Application.Json)
            setBody(updateBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Updated Client"))
        assertTrue(responseBody.contains("updated@email.com"))
    }

    @Test
    fun `DELETE appointments should remove appointment`() = testApplication {
        setupApp()

        // Create appointment first
        val appointmentRequest = AppointmentRequest("Test Client", "test@email.com", "2025-07-15T10:00:00", 1)
        val requestBody = Json.encodeToString(appointmentRequest)

        client.post("/appointments") {
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        // Delete appointment
        val response = client.delete("/appointments/1")

        assertEquals(HttpStatusCode.NoContent, response.status)

        // Verify it's deleted
        val getResponse = client.get("/appointments/1")
        assertEquals(HttpStatusCode.NotFound, getResponse.status)
    }
}