package com.example.hotel.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateBookingRequest(
        @NotNull @Positive Long roomTypeId,
        @NotNull LocalDate checkInDate,
        @NotNull LocalDate checkOutDate,
        @NotNull @Positive Integer roomCount,
        @NotBlank @Size(max = 100) String guestName,
        @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$") String guestPhone) {}
