package com.example.hotel.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "booking_order",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_booking_order_no", columnNames = "order_no"),
            @UniqueConstraint(name = "uk_booking_idempotency", columnNames = "idempotency_key")
        },
        indexes = @Index(name = "idx_booking_phone_created", columnList = "guest_phone,created_at"))
public class BookingOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 32, updatable = false)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RoomType roomType;

    @Column(nullable = false, length = 100)
    private String hotelNameSnapshot;

    @Column(nullable = false, length = 100)
    private String roomTypeNameSnapshot;

    @Column(nullable = false, length = 50)
    private String bedTypeSnapshot;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "guest_name", nullable = false, length = 100)
    private String guestName;

    @Column(name = "guest_phone", nullable = false, length = 20)
    private String guestPhone;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false)
    private int roomCount;

    @Column(nullable = false)
    private int nightCount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 64, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false, length = 64, updatable = false)
    private String requestHash;

    private OffsetDateTime checkedInAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private StaffUser checkedInBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Version private long version;

    protected BookingOrder() {}

    public BookingOrder(
            String orderNo,
            Hotel hotel,
            RoomType roomType,
            String guestName,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            int rooms,
            int nights,
            BigDecimal total,
            String key,
            String hash,
            OffsetDateTime now) {
        this.orderNo = orderNo;
        this.hotel = hotel;
        this.roomType = roomType;
        hotelNameSnapshot = hotel.getName();
        roomTypeNameSnapshot = roomType.getName();
        bedTypeSnapshot = roomType.getBedType();
        unitPriceSnapshot = roomType.getBasePrice();
        this.guestName = guestName;
        this.guestPhone = guestPhone;
        checkInDate = checkIn;
        checkOutDate = checkOut;
        roomCount = rooms;
        nightCount = nights;
        totalAmount = total;
        status = OrderStatus.BOOKED;
        idempotencyKey = key;
        requestHash = hash;
        createdAt = updatedAt = now;
    }

    public void checkIn(StaffUser staff, OffsetDateTime now) {
        status = OrderStatus.CHECKED_IN;
        checkedInBy = staff;
        checkedInAt = now;
        updatedAt = now;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getHotelNameSnapshot() {
        return hotelNameSnapshot;
    }

    public String getRoomTypeNameSnapshot() {
        return roomTypeNameSnapshot;
    }

    public String getBedTypeSnapshot() {
        return bedTypeSnapshot;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestPhone() {
        return guestPhone;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public int getRoomCount() {
        return roomCount;
    }

    public int getNightCount() {
        return nightCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public StaffUser getCheckedInBy() {
        return checkedInBy;
    }
}
