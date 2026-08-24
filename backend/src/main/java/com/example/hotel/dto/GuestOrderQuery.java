package com.example.hotel.dto;
import jakarta.validation.constraints.*;
public record GuestOrderQuery(@NotBlank @Size(max=32) String orderNo,@NotBlank @Pattern(regexp="^1[3-9]\\d{9}$") String guestPhone){}
