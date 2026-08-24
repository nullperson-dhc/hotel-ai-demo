package com.example.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import com.example.hotel.dto.*; import com.example.hotel.repository.BookingOrderRepository; import com.example.hotel.service.*; import java.time.*; import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookingFlowTest{
 @Autowired AvailabilityService availability; @Autowired BookingService bookings; @Autowired BookingOrderRepository orders; @Autowired Clock clock;
 @Test void bookingIsQueryableAndIdempotent(){LocalDate in=LocalDate.now(clock).plusDays(1),out=in.plusDays(2);var available=availability.find(in,out,1);assertThat(available.roomTypes()).isNotEmpty();Long room=available.roomTypes().get(0).id();var req=new CreateBookingRequest(room,in,out,1,"测试顾客","13800138000");var first=bookings.create("test-idempotent-key",req);var retry=bookings.create("test-idempotent-key",req);assertThat(first.created()).isTrue();assertThat(retry.created()).isFalse();assertThat(retry.order().orderNo()).isEqualTo(first.order().orderNo());assertThat(orders.count()).isEqualTo(1);assertThat(bookings.guestQuery(new GuestOrderQuery(first.order().orderNo(),"13800138000")).guestPhoneMasked()).isEqualTo("138****8000");}
}
