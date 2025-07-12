package com.example.plugins

import com.example.models.*
import com.example.repositories.AppointmentRepository
import com.example.repositories.ServiceRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // Services Routes
        route("/services") {
            // GET /services: ดึงบริการทั้งหมด
            get {
                val services = ServiceRepository.getAll()
                call.respond(HttpStatusCode.OK, services)
            }

            // GET /services/{id}: ดึงบริการตาม ID
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid service ID"))
                    return@get
                }

                val service = ServiceRepository.getById(id)
                if (service != null) {
                    call.respond(HttpStatusCode.OK, service)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Service not found"))
                }
            }

            // POST /services: สร้างบริการใหม่
            post {
                try {
                    val serviceRequest = call.receive<ServiceRequest>()

                    // Validation
                    if (serviceRequest.name.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Service name cannot be empty"))
                        return@post
                    }
                    if (serviceRequest.defaultDurationInMinutes <= 0) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Duration must be greater than 0"))
                        return@post
                    }

                    val newService = ServiceRepository.add(serviceRequest)
                    call.respond(HttpStatusCode.Created, newService)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                }
            }

            // PUT /services/{id}: อัพเดทบริการ
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid service ID"))
                    return@put
                }

                try {
                    val serviceRequest = call.receive<ServiceRequest>()

                    // Validation
                    if (serviceRequest.name.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Service name cannot be empty"))
                        return@put
                    }
                    if (serviceRequest.defaultDurationInMinutes <= 0) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Duration must be greater than 0"))
                        return@put
                    }

                    val updatedService = ServiceRepository.update(id, serviceRequest)
                    if (updatedService != null) {
                        call.respond(HttpStatusCode.OK, updatedService)
                    } else {
                        call.respond(HttpStatusCode.NotFound, ErrorResponse("Service not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                }
            }

            // DELETE /services/{id}: ลบบริการ
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid service ID"))
                    return@delete
                }

                val deleted = ServiceRepository.delete(id)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Service not found"))
                }
            }
        }

        // Appointments Routes
        route("/appointments") {
            // GET /appointments: ดึงการนัดหมายทั้งหมด
            get {
                val appointments = AppointmentRepository.getAll()
                call.respond(HttpStatusCode.OK, appointments)
            }

            // GET /appointments/{id}: ดึงการนัดหมายตาม ID
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid appointment ID"))
                    return@get
                }

                val appointment = AppointmentRepository.getById(id)
                if (appointment != null) {
                    call.respond(HttpStatusCode.OK, appointment)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Appointment not found"))
                }
            }

            // POST /appointments: สร้างการนัดหมายใหม่
            post {
                try {
                    val appointmentRequest = call.receive<AppointmentRequest>()

                    // Validation
                    if (appointmentRequest.clientName.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Client name cannot be empty"))
                        return@post
                    }
                    if (appointmentRequest.clientEmail.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Client email cannot be empty"))
                        return@post
                    }

                    val result = AppointmentRepository.add(appointmentRequest)
                    result.fold(
                        onSuccess = { appointment ->
                            call.respond(HttpStatusCode.Created, appointment)
                        },
                        onFailure = { exception ->
                            when (exception.message) {
                                "Service not found" -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(exception.message!!))
                                "Time slot is already booked. Please choose a different time." -> call.respond(HttpStatusCode.Conflict, ErrorResponse(exception.message!!))
                                else -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(exception.message ?: "Invalid request"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                }
            }

            // PUT /appointments/{id}: อัพเดทการนัดหมาย
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid appointment ID"))
                    return@put
                }

                try {
                    val appointmentRequest = call.receive<AppointmentRequest>()

                    // Validation
                    if (appointmentRequest.clientName.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Client name cannot be empty"))
                        return@put
                    }
                    if (appointmentRequest.clientEmail.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Client email cannot be empty"))
                        return@put
                    }

                    val result = AppointmentRepository.update(id, appointmentRequest)
                    result.fold(
                        onSuccess = { appointment ->
                            if (appointment != null) {
                                call.respond(HttpStatusCode.OK, appointment)
                            } else {
                                call.respond(HttpStatusCode.NotFound, ErrorResponse("Appointment not found"))
                            }
                        },
                        onFailure = { exception ->
                            when (exception.message) {
                                "Service not found" -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(exception.message!!))
                                "Time slot is already booked. Please choose a different time." -> call.respond(HttpStatusCode.Conflict, ErrorResponse(exception.message!!))
                                else -> call.respond(HttpStatusCode.BadRequest, ErrorResponse(exception.message ?: "Invalid request"))
                            }
                        }
                    )
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request body"))
                }
            }

            // DELETE /appointments/{id}: ลบการนัดหมาย
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid appointment ID"))
                    return@delete
                }

                val deleted = AppointmentRepository.delete(id)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Appointment not found"))
                }
            }
        }
    }
}