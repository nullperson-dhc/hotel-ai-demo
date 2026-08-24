package com.example.hotel.dto;

import com.example.hotel.domain.BookingOrder;
import java.time.*;

public record OrderResponse(
        String orderNo,
        String hotelName,
        String roomTypeName,
        String bedType,
        String guestName,
        String guestPhoneMasked,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int roomCount,
        int nightCount,
        String unitPrice,
        String totalAmount,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime checkedInAt,
        String checkedInByName) {
    public static OrderResponse from(BookingOrder o) {
        String p = o.getGuestPhone();
        String masked = p.substring(0, 3) + "****" + p.substring(7);
        return new OrderResponse(
                o.getOrderNo(),
                o.getHotelNameSnapshot(),
                o.getRoomTypeNameSnapshot(),
                o.getBedTypeSnapshot(),
                o.getGuestName(),
                masked,
                o.getCheckInDate(),
                o.getCheckOutDate(),
                o.getRoomCount(),
                o.getNightCount(),
                o.getUnitPriceSnapshot().toPlainString(),
                o.getTotalAmount().toPlainString(),
                o.getStatus().name(),
                o.getCreatedAt(),
                o.getCheckedInAt(),
                o.getCheckedInBy() == null ? null : o.getCheckedInBy().getDisplayName());
    }
}
