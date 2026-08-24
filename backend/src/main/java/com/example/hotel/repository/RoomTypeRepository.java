package com.example.hotel.repository;

import com.example.hotel.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByHotelIdAndStatus(Long hotelId, Status status);

    Optional<RoomType> findByIdAndStatus(Long id, Status status);
}
