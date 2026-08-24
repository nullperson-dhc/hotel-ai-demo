package com.example.hotel.service;

import com.alibaba.cola.statemachine.*;
import com.example.hotel.config.OrderStateMachineConfig;
import com.example.hotel.domain.*;
import com.example.hotel.dto.*;
import com.example.hotel.exception.BusinessException;
import com.example.hotel.repository.*;
import java.time.*;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffBookingService {
    private final BookingOrderRepository orders;
    private final StaffUserRepository staff;
    private final Clock clock;

    public StaffBookingService(BookingOrderRepository o, StaffUserRepository s, Clock c) {
        orders = o;
        staff = s;
        clock = c;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> search(String orderNo, String phone, int page, int size) {
        boolean a = orderNo != null && !orderNo.isBlank(), b = phone != null && !phone.isBlank();
        if (a == b)
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "订单号和手机号必须且只能提供一个");
        if (a) {
            List<OrderResponse> items =
                    orders.findByOrderNo(orderNo).map(OrderResponse::from).stream().toList();
            return new PageResponse<>(items, 0, size, items.size(), items.isEmpty() ? 0 : 1);
        }
        Page<BookingOrder> p =
                orders.findByGuestPhoneOrderByCreatedAtDescOrderNoDesc(
                        phone, PageRequest.of(page, size));
        return new PageResponse<>(
                p.stream().map(OrderResponse::from).toList(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public OrderResponse detail(String no) {
        return OrderResponse.from(find(no));
    }

    @Transactional
    public OrderResponse checkIn(String no, String username) {
        BookingOrder order = find(no);
        StateMachine<OrderStatus, OrderEvent, Object> machine =
                StateMachineFactory.get(OrderStateMachineConfig.MACHINE_ID);
        OrderStatus current = order.getStatus();
        OrderStatus next = machine.fireEvent(current, OrderEvent.CHECK_IN, null);
        if (next == current)
            throw new BusinessException(
                    HttpStatus.CONFLICT, "ORDER_STATUS_CONFLICT", "订单状态不允许办理入住");
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(order.getCheckInDate()))
            throw new BusinessException(HttpStatus.CONFLICT, "CHECK_IN_TOO_EARLY", "尚未到入住日期");
        if (!today.isBefore(order.getCheckOutDate()))
            throw new BusinessException(HttpStatus.CONFLICT, "CHECK_IN_EXPIRED", "订单已过入住有效期");
        StaffUser operator =
                staff.findByUsername(username)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录"));
        order.checkIn(operator, OffsetDateTime.now(clock));
        return OrderResponse.from(order);
    }

    private BookingOrder find(String no) {
        return orders.findByOrderNo(no)
                .orElseThrow(
                        () ->
                                new BusinessException(
                                        HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在"));
    }
}
