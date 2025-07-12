package com.example.models

import kotlinx.serialization.Serializable

// Data Classes for Services
@Serializable
data class Service(
    val id: Int,
    val name: String,
    val description: String,
    val defaultDurationInMinutes: Int
)

@Serializable
data class ServiceRequest(
    val name: String,
    val description: String,
    val defaultDurationInMinutes: Int
)

// Data Classes for Appointments
@Serializable
data class Appointment(
    val id: Int,
    val clientName: String,
    val clientEmail: String,
    val appointmentTime: String, // ISO 8601 format (yyyy-MM-ddTHH:mm:ss)
    val serviceId: Int
)

@Serializable
data class AppointmentRequest(
    val clientName: String,
    val clientEmail: String,
    val appointmentTime: String,
    val serviceId: Int
)

// Response Class for Errors
@Serializable
data class ErrorResponse(val error: String)