package com.example.hotel.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
public class Hotel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=100) private String name;
    @Column(nullable=false, length=255) private String address;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Status status;
    @Column(nullable=false) private OffsetDateTime createdAt;
    @Column(nullable=false) private OffsetDateTime updatedAt;
    @Version private long version;
    protected Hotel() {}
    public Hotel(String name, String address) { this.name=name; this.address=address; status=Status.ACTIVE; createdAt=updatedAt=OffsetDateTime.now(); }
    public Long getId(){return id;} public String getName(){return name;} public String getAddress(){return address;} public Status getStatus(){return status;}
}
