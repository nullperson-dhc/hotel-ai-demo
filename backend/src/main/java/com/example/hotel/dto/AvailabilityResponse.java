package com.example.hotel.dto;
import java.time.LocalDate; import java.util.List;
public record AvailabilityResponse(HotelSummary hotel,LocalDate checkInDate,LocalDate checkOutDate,int nightCount,int roomCount,List<RoomTypeSummary> roomTypes){public record HotelSummary(Long id,String name,String address){} public record RoomTypeSummary(Long id,String name,String bedType,int capacity,String description,String unitPrice,String estimatedTotalAmount){}}
