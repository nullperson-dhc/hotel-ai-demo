package com.example.hotel.service;

import com.example.hotel.domain.*;
import com.example.hotel.dto.*;
import com.example.hotel.exception.BusinessException;
import com.example.hotel.repository.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final RoomTypeRepository rooms;
    private final RoomInventoryRepository inventory;
    private final BookingOrderRepository orders;
    private final StayValidator validator;
    private final Clock clock;

    public BookingService(
            RoomTypeRepository r,
            RoomInventoryRepository i,
            BookingOrderRepository o,
            StayValidator v,
            Clock c) {
        rooms = r;
        inventory = i;
        orders = o;
        validator = v;
        clock = c;
    }

    @Transactional
    public BookingResult create(String key, CreateBookingRequest raw) {
        if (key == null || key.isBlank() || key.length() > 64)
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Idempotency-Key 必填且不能超过64字符");
        String name = raw.guestName().trim();
        String hash =
                hash(
                        raw.roomTypeId()
                                + "|"
                                + raw.checkInDate()
                                + "|"
                                + raw.checkOutDate()
                                + "|"
                                + raw.roomCount()
                                + "|"
                                + name
                                + "|"
                                + raw.guestPhone());
        var previous = orders.findByIdempotencyKey(key);
        if (previous.isPresent()) {
            if (!previous.get().getRequestHash().equals(hash))
                throw new BusinessException(
                        HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "幂等键已用于其他请求");
            return new BookingResult(OrderResponse.from(previous.get()), false);
        }
        int nights = validator.validate(raw.checkInDate(), raw.checkOutDate());
        RoomType room =
                rooms.findByIdAndStatus(raw.roomTypeId(), Status.ACTIVE)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                HttpStatus.NOT_FOUND,
                                                "ROOM_TYPE_NOT_FOUND",
                                                "房型不存在或不可预订"));
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (LocalDate d = raw.checkInDate(); d.isBefore(raw.checkOutDate()); d = d.plusDays(1)) {
            if (inventory.decrement(room.getId(), d, raw.roomCount(), now) != 1) {
                log.info("Inventory insufficient roomType={} date={}", room.getId(), d);
                throw new BusinessException(
                        HttpStatus.CONFLICT, "INVENTORY_INSUFFICIENT", "所选日期库存不足");
            }
        }
        BigDecimal total =
                room.getBasePrice()
                        .multiply(BigDecimal.valueOf((long) nights * raw.roomCount()))
                        .setScale(2, RoundingMode.HALF_UP);
        String no =
                "H"
                        + LocalDate.now(clock).toString().replace("-", "")
                        + UUID.randomUUID()
                                .toString()
                                .replace("-", "")
                                .substring(0, 8)
                                .toUpperCase();
        BookingOrder order =
                orders.save(
                        new BookingOrder(
                                no,
                                room.getHotel(),
                                room,
                                name,
                                raw.guestPhone(),
                                raw.checkInDate(),
                                raw.checkOutDate(),
                                raw.roomCount(),
                                nights,
                                total,
                                key,
                                hash,
                                now));
        log.info("Booking created orderNo={}", no);
        return new BookingResult(OrderResponse.from(order), true);
    }

    @Transactional(readOnly = true)
    public OrderResponse guestQuery(GuestOrderQuery q) {
        return orders.findByOrderNoAndGuestPhone(q.orderNo(), q.guestPhone())
                .map(OrderResponse::from)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "未找到匹配订单"));
    }

    private String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record BookingResult(OrderResponse order, boolean created) {}
}
