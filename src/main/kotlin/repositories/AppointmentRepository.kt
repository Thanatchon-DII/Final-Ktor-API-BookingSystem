package com.example.repositories

import com.example.models.Appointment
import com.example.models.AppointmentRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object AppointmentRepository {
    // โค้ด AppointmentRepository เดิม
    private val appointments = mutableListOf<Appointment>()
    private var nextId = 1

    fun getAll(): List<Appointment> = appointments.toList()

    fun getById(id: Int): Appointment? = appointments.find { it.id == id }

    fun add(appointmentRequest: AppointmentRequest): Result<Appointment> {
        // ตรวจสอบว่า service มีอยู่จริงหรือไม่
        val service = ServiceRepository.getById(appointmentRequest.serviceId)
            ?: return Result.failure(Exception("Service not found"))

        // ตรวจสอบ format ของเวลา
        val appointmentDateTime = try {
            LocalDateTime.parse(appointmentRequest.appointmentTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: DateTimeParseException) {
            return Result.failure(Exception("Invalid date time format. Use yyyy-MM-ddTHH:mm:ss"))
        }

        // ตรวจสอบการจองซ้อน (Double Booking)
        val endTime = appointmentDateTime.plusMinutes(service.defaultDurationInMinutes.toLong())

        val hasConflict = appointments.any { existingAppointment ->
            val existingDateTime = LocalDateTime.parse(existingAppointment.appointmentTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val existingService = ServiceRepository.getById(existingAppointment.serviceId)

            if (existingService != null) {
                val existingEndTime = existingDateTime.plusMinutes(existingService.defaultDurationInMinutes.toLong())

                // ตรวจสอบว่าช่วงเวลาทับซ้อนกันหรือไม่
                !(appointmentDateTime >= existingEndTime || endTime <= existingDateTime)
            } else false
        }

        if (hasConflict) {
            return Result.failure(Exception("Time slot is already booked. Please choose a different time."))
        }

        // สร้าง appointment ใหม่
        val newAppointment = Appointment(
            id = nextId++,
            clientName = appointmentRequest.clientName,
            clientEmail = appointmentRequest.clientEmail,
            appointmentTime = appointmentRequest.appointmentTime,
            serviceId = appointmentRequest.serviceId
        )
        appointments.add(newAppointment)
        return Result.success(newAppointment)
    }

    fun update(id: Int, appointmentRequest: AppointmentRequest): Result<Appointment?> {
        val index = appointments.indexOfFirst { it.id == id }
        if (index == -1) {
            return Result.success(null)
        }

        // ตรวจสอบว่า service มีอยู่จริงหรือไม่
        val service = ServiceRepository.getById(appointmentRequest.serviceId)
            ?: return Result.failure(Exception("Service not found"))

        // ตรวจสอบ format ของเวลา
        val appointmentDateTime = try {
            LocalDateTime.parse(appointmentRequest.appointmentTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: DateTimeParseException) {
            return Result.failure(Exception("Invalid date time format. Use yyyy-MM-ddTHH:mm:ss"))
        }

        // ตรวจสอบการจองซ้อน (ยกเว้น appointment ที่กำลังอัพเดท)
        val endTime = appointmentDateTime.plusMinutes(service.defaultDurationInMinutes.toLong())

        val hasConflict = appointments.any { existingAppointment ->
            if (existingAppointment.id == id) return@any false // ข้าม appointment ที่กำลังอัพเดท

            val existingDateTime = LocalDateTime.parse(existingAppointment.appointmentTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val existingService = ServiceRepository.getById(existingAppointment.serviceId)

            if (existingService != null) {
                val existingEndTime = existingDateTime.plusMinutes(existingService.defaultDurationInMinutes.toLong())

                // ตรวจสอบว่าช่วงเวลาทับซ้อนกันหรือไม่
                !(appointmentDateTime >= existingEndTime || endTime <= existingDateTime)
            } else false
        }

        if (hasConflict) {
            return Result.failure(Exception("Time slot is already booked. Please choose a different time."))
        }

        val updatedAppointment = Appointment(
            id = id,
            clientName = appointmentRequest.clientName,
            clientEmail = appointmentRequest.clientEmail,
            appointmentTime = appointmentRequest.appointmentTime,
            serviceId = appointmentRequest.serviceId
        )
        appointments[index] = updatedAppointment
        return Result.success(updatedAppointment)
    }

    fun delete(id: Int): Boolean {
        return appointments.removeIf { it.id == id }
    }

    fun reset() {
        appointments.clear()
        nextId = 1
    }
}