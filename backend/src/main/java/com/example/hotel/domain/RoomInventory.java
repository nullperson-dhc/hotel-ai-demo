package com.example.hotel.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_inventory_room_date",
                        columnNames = {"room_type_id", "biz_date"}))
public class RoomInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private RoomType roomType;

    @Column(name = "biz_date", nullable = false)
    private LocalDate bizDate;

    @Column(nullable = false)
    private int totalStock;

    @Column(nullable = false)
    private int availableStock;

    @Version private long version;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected RoomInventory() {}

    public RoomInventory(RoomType roomType, LocalDate bizDate, int stock) {
        this.roomType = roomType;
        this.bizDate = bizDate;
        this.totalStock = stock;
        this.availableStock = stock;
        createdAt = updatedAt = OffsetDateTime.now();
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public LocalDate getBizDate() {
        return bizDate;
    }
}
