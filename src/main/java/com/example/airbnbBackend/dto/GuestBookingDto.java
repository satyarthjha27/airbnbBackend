package com.example.airbnbBackend.dto;

import com.example.airbnbBackend.entity.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Safe booking-history representation for the currently authenticated customer.
 * It intentionally excludes payment-session identifiers and user credentials.
 */
@Data
@Builder
public class GuestBookingDto {
    private Long id;
    private Integer roomsCount;
    private Integer guestCapacity;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime createdAt;
    private BookingStatus bookingStatus;
    private BigDecimal amount;
    private Long hotelId;
    private String hotelName;
    private String hotelCity;
    private String hotelPhoto;
    private Long roomId;
    private String roomType;
    private Set<GuestDto> guests;
}
