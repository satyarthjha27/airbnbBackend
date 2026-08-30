package com.example.airbnbBackend.services;

import com.example.airbnbBackend.dto.BookingDto;
import com.example.airbnbBackend.dto.BookingQuoteDto;
import com.example.airbnbBackend.dto.BookingRequest;
import com.example.airbnbBackend.dto.GuestBookingDto;
import com.example.airbnbBackend.dto.GuestDto;
import com.example.airbnbBackend.entity.enums.BookingStatus;
import com.stripe.model.Event;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface BookingService {
    BookingDto initialiseBooking(BookingRequest bookingRequest);

    BookingQuoteDto getBookingQuote(BookingRequest bookingRequest);

    BookingDto addGuest(Long bookingId, List<GuestDto> guestDto);

    String initialiseCheckout(Long bookingId);

    void capturePayment(Event event);

    void cancelBooking(Long bookingId);

    String getBookingStatus(Long bookingId);

    Page<GuestBookingDto> getMyBookings(BookingStatus status, Pageable pageable);

    List<BookingDto> getAllBookingOfHotel(Long hotelId);
}
