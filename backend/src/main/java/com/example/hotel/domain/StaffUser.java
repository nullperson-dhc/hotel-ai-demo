package com.example.hotel.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(name="uk_staff_username",columnNames="username"))
public class StaffUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=64) private String username;
    @Column(nullable=false,length=100) private String passwordHash;
    @Column(nullable=false,length=100) private String displayName;
    @Column(nullable=false,length=20) private String role="STAFF";
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
    @Column(nullable=false) private OffsetDateTime createdAt;
    @Column(nullable=false) private OffsetDateTime updatedAt;
    @Version private long version;
    protected StaffUser() {}
    public StaffUser(String username,String passwordHash,String displayName){this.username=username;this.passwordHash=passwordHash;this.displayName=displayName;status=Status.ACTIVE;createdAt=updatedAt=OffsetDateTime.now();}
    public Long getId(){return id;} public String getUsername(){return username;} public String getPasswordHash(){return passwordHash;} public String getDisplayName(){return displayName;} public Status getStatus(){return status;}
}
