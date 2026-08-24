package com.example.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.hotel.domain.BookingOrder;
import com.example.hotel.dto.CreateBookingRequest;
import com.example.hotel.dto.GuestOrderQuery;
import com.example.hotel.exception.BusinessException;
import com.example.hotel.repository.BookingOrderRepository;
import com.example.hotel.repository.HotelRepository;
import com.example.hotel.repository.RoomInventoryRepository;
import com.example.hotel.repository.RoomTypeRepository;
import com.example.hotel.service.AvailabilityService;
import com.example.hotel.service.BookingService;
import com.example.hotel.service.StaffBookingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HotelAcceptanceTest {
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Shanghai"));

    @Autowired BookingService bookings;
    @Autowired AvailabilityService availability;
    @Autowired StaffBookingService staffBookings;
    @Autowired BookingOrderRepository orders;
    @Autowired RoomInventoryRepository inventory;
    @Autowired RoomTypeRepository roomTypes;
    @Autowired HotelRepository hotels;

    @Test
    void normalBookingDeductsEveryOccupiedNightAndIsQueryable() {
        LocalDate in = TODAY.plusDays(30), out = in.plusDays(3);
        Long roomId = firstRoomId(in, out);
        List<Integer> before = stocks(roomId, in, out.plusDays(1));

        var result = bookings.create(key(), request(roomId, in, out, 1, "正常预订", "13810000001"));

        assertThat(result.order().status()).isEqualTo("BOOKED");
        assertThat(result.order().nightCount()).isEqualTo(3);
        assertThat(result.order().totalAmount()).isEqualTo("1197.00");
        assertThat(stocks(roomId, in, out.plusDays(1)))
                .containsExactly(before.get(0) - 1, before.get(1) - 1, before.get(2) - 1, before.get(3));
        assertThat(bookings.guestQuery(new GuestOrderQuery(result.order().orderNo(), "13810000001")).orderNo())
                .isEqualTo(result.order().orderNo());
    }

    @Test
    void insufficientInventoryDoesNotCreateOrder() {
        LocalDate in = TODAY.plusDays(40), out = in.plusDays(2);
        Long roomId = firstRoomId(in, out);
        long beforeOrders = orders.count();
        List<Integer> before = stocks(roomId, in, out);

        assertBusinessCode(() -> bookings.create(key(), request(roomId, in, out, 6, "库存不足", "13810000002")), "INVENTORY_INSUFFICIENT");

        assertThat(orders.count()).isEqualTo(beforeOrders);
        assertThat(stocks(roomId, in, out)).isEqualTo(before);
    }

    @Test
    void invalidStayPeriodsAreRejected() {
        Long roomId = firstRoomId(TODAY.plusDays(50), TODAY.plusDays(51));
        assertBusinessCode(() -> bookings.create(key(), request(roomId, TODAY, TODAY, 1, "同日离店", "13810000003")), "INVALID_STAY_PERIOD");
        assertBusinessCode(() -> bookings.create(key(), request(roomId, TODAY.minusDays(1), TODAY.plusDays(1), 1, "过去日期", "13810000004")), "INVALID_STAY_PERIOD");
    }

    @Test
    void oneSoldOutNightRollsBackDeductionsForOtherNights() {
        LocalDate in = TODAY.plusDays(60), out = in.plusDays(3);
        Long roomId = firstRoomId(in, out);
        inventory.decrement(roomId, in.plusDays(1), 5, OffsetDateTime.now());

        assertBusinessCode(() -> bookings.create(key(), request(roomId, in, out, 1, "跨夜回滚", "13810000005")), "INVENTORY_INSUFFICIENT");

        assertThat(stocks(roomId, in, out)).containsExactly(5, 0, 5);
        assertThat(availability.find(in, out, 1).roomTypes()).noneMatch(room -> room.id().equals(roomId));
    }

    @Test
    void missingOrderAndWrongPhoneUseSameError() {
        LocalDate in = TODAY.plusDays(70), out = in.plusDays(1);
        Long roomId = firstRoomId(in, out);
        var order = bookings.create(key(), request(roomId, in, out, 1, "安全查单", "13810000006")).order();

        assertBusinessCode(() -> bookings.guestQuery(new GuestOrderQuery(order.orderNo(), "13810009999")), "ORDER_NOT_FOUND");
        assertBusinessCode(() -> bookings.guestQuery(new GuestOrderQuery("H不存在00000000", "13810000006")), "ORDER_NOT_FOUND");
    }

    @Test
    void checkInSucceedsOnceAndRepeatIsRejected() {
        Long roomId = firstRoomId(TODAY, TODAY.plusDays(1));
        var order = bookings.create(key(), request(roomId, TODAY, TODAY.plusDays(1), 1, "正常入住", "13810000007")).order();
        int stockAfterBooking = stocks(roomId, TODAY, TODAY.plusDays(1)).get(0);

        var checkedIn = staffBookings.checkIn(order.orderNo(), "frontdesk");
        assertThat(checkedIn.status()).isEqualTo("CHECKED_IN");
        assertThat(checkedIn.checkedInAt()).isNotNull();
        assertBusinessCode(() -> staffBookings.checkIn(order.orderNo(), "frontdesk"), "ORDER_STATUS_CONFLICT");
        assertThat(stocks(roomId, TODAY, TODAY.plusDays(1))).containsExactly(stockAfterBooking);
    }

    @Test
    void checkInRejectsTooEarlyAndExpiredOrders() {
        Long roomId = firstRoomId(TODAY.plusDays(80), TODAY.plusDays(81));
        var future = bookings.create(key(), request(roomId, TODAY.plusDays(80), TODAY.plusDays(81), 1, "提前入住", "13810000008")).order();
        assertBusinessCode(() -> staffBookings.checkIn(future.orderNo(), "frontdesk"), "CHECK_IN_TOO_EARLY");

        var room = roomTypes.findById(roomId).orElseThrow();
        var expired = orders.save(new BookingOrder("H20260820EXPIRED", hotels.findAll().get(0), room, "过期入住", "13810000009",
                TODAY.minusDays(2), TODAY, 1, 2, new BigDecimal("798.00"), key(), "expired-hash", OffsetDateTime.now()));
        assertBusinessCode(() -> staffBookings.checkIn(expired.getOrderNo(), "frontdesk"), "CHECK_IN_EXPIRED");
    }

    @Test
    void concurrentBookingOfLastRoomAllowsOnlyOneSuccess() throws Exception {
        LocalDate in = TODAY.plusDays(90), out = in.plusDays(1);
        Long roomId = firstRoomId(in, out);
        inventory.decrement(roomId, in, 4, OffsetDateTime.now());
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        List<String> outcomes = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 2; i++) {
            final int n = i;
            executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    bookings.create(key(), request(roomId, in, out, 1, "并发顾客" + n, "1381000001" + n));
                    outcomes.add("SUCCESS");
                } catch (BusinessException e) {
                    outcomes.add(e.getCode());
                }
                return null;
            });
        }
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(outcomes).containsExactlyInAnyOrder("SUCCESS", "INVENTORY_INSUFFICIENT");
        assertThat(stocks(roomId, in, out)).containsExactly(0);
    }

    private Long firstRoomId(LocalDate in, LocalDate out) { return availability.find(in, out, 1).roomTypes().get(0).id(); }
    private List<Integer> stocks(Long roomId, LocalDate in, LocalDate out) { return inventory.findByRoomTypeIdAndBizDateGreaterThanEqualAndBizDateLessThan(roomId, in, out).stream().map(i -> i.getAvailableStock()).toList(); }
    private CreateBookingRequest request(Long roomId, LocalDate in, LocalDate out, int count, String name, String phone) { return new CreateBookingRequest(roomId, in, out, count, name, phone); }
    private String key() { return UUID.randomUUID().toString(); }
    private void assertBusinessCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) { assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(code)); }
}
