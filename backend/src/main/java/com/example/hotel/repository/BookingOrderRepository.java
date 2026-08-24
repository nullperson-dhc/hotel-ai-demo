package com.example.hotel.repository;
import com.example.hotel.domain.BookingOrder; import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*;
public interface BookingOrderRepository extends JpaRepository<BookingOrder,Long>{Optional<BookingOrder> findByOrderNo(String orderNo);Optional<BookingOrder> findByOrderNoAndGuestPhone(String orderNo,String phone);Optional<BookingOrder> findByIdempotencyKey(String key);Page<BookingOrder> findByGuestPhoneOrderByCreatedAtDescOrderNoDesc(String phone,Pageable pageable);}
