package com.example.hotel.repository;
import com.example.hotel.domain.*; import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
public interface HotelRepository extends JpaRepository<Hotel,Long>{Optional<Hotel> findFirstByStatus(Status status);}
