package com.example.airbnbBackend.controller;

import com.example.airbnbBackend.dto.BookingDto;
import com.example.airbnbBackend.dto.BookingRequest;
import com.example.airbnbBackend.dto.GuestBookingDto;
import com.example.airbnbBackend.dto.GuestDto;
import com.example.airbnbBackend.entity.enums.BookingStatus;
import com.example.airbnbBackend.services.BookingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/bookings")
@AllArgsConstructor
public class HotelBookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingDto> initialiseBooking(@RequestBody BookingRequest bookingRequest){
        return ResponseEntity.ok(bookingService.initialiseBooking(bookingRequest));
    }

    @PostMapping("/{bookingId}/addguest")
    public ResponseEntity<BookingDto> addGuests(@PathVariable Long bookingId, @RequestBody List<GuestDto> guestDto){
        return ResponseEntity.ok(bookingService.addGuest(bookingId, guestDto));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<GuestBookingDto>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(bookingService.getMyBookings(status, pageable));
    }

    @PostMapping("/{bookingId}/payment")
    public ResponseEntity<Map<String, String>> initialiseCheckout(@PathVariable Long bookingId){
        String sessionUrl = bookingService.initialiseCheckout(bookingId);
        return ResponseEntity.ok(Map.of("sessionUrl",sessionUrl));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId){
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{bookingId}/status")
    public ResponseEntity<Map<String,String>> getBookingStatus(@PathVariable Long bookingId){
        return ResponseEntity.ok(Map.of("Status: ", bookingService.getBookingStatus(bookingId)));
    }
}
