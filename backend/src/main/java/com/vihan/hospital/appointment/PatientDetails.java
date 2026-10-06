package com.vihan.hospital.appointment;

/**
 * Patient details as entered on a booking, already cleaned up (trimmed, mobile normalised to 10 digits).
 *
 * @param gender M, F or O
 */
public record PatientDetails(String name, Integer age, String gender, String mobile, String reason) {
}
