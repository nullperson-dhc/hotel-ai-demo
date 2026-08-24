package com.example.hotel.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(name="uk_room_type_hotel_name", columnNames={"hotel_id","name"}))
public class RoomType {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) private Hotel hotel;
    @Column(nullable=false, length=100) private String name;
    @Column(nullable=false, length=50) private String bedType;
    @Column(nullable=false) private int capacity;
    @Column(length=1000) private String description;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal basePrice;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Status status;
    @Column(nullable=false) private OffsetDateTime createdAt;
    @Column(nullable=false) private OffsetDateTime updatedAt;
    @Version private long version;
    protected RoomType() {}
    public RoomType(Hotel hotel,String name,String bedType,int capacity,String description,BigDecimal basePrice){this.hotel=hotel;this.name=name;this.bedType=bedType;this.capacity=capacity;this.description=description;this.basePrice=basePrice;status=Status.ACTIVE;createdAt=updatedAt=OffsetDateTime.now();}
    public Long getId(){return id;} public Hotel getHotel(){return hotel;} public String getName(){return name;} public String getBedType(){return bedType;} public int getCapacity(){return capacity;} public String getDescription(){return description;} public BigDecimal getBasePrice(){return basePrice;} public Status getStatus(){return status;}
}
