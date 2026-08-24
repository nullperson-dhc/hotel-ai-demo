package com.example.hotel.dto;

public record SessionResponse(boolean authenticated, Staff staff, Csrf csrf) {
    public record Staff(String displayName) {}

    public record Csrf(String headerName, String token) {}
}
