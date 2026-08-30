package com.example.airbnbBackend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A non-binding server-calculated rate. Booking creation is authoritative because
 * availability and dynamic pricing may change before inventory is locked.
 */
@Data
@Builder
public class BookingQuoteDto {
    private Long hotelId;
    private Long roomId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer roomsCount;
    private Long nights;
    private String currency;
    private List<NightlyRateDto> nightlyRates;
    private BigDecimal total;
    private LocalDateTime quoteExpiresAt;
}
