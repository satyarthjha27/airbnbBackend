package com.example.airbnbBackend.controller;


import com.example.airbnbBackend.dto.BookingQuoteDto;
import com.example.airbnbBackend.dto.BookingRequest;
import com.example.airbnbBackend.dto.HotelDto;
import com.example.airbnbBackend.dto.HotelInfoDto;
import com.example.airbnbBackend.dto.HotelPriceDto;
import com.example.airbnbBackend.dto.HotelSearchRequest;
import com.example.airbnbBackend.services.BookingService;
import com.example.airbnbBackend.services.HotelService;
import com.example.airbnbBackend.services.InventoryService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/hotels")
public class HotelBrowseContoller {

    private final InventoryService inventoryService;
    private final HotelService hotelService;
    private final BookingService bookingService;

    @GetMapping("/search")
    public ResponseEntity<Page<HotelPriceDto>> searchHotels(@ModelAttribute HotelSearchRequest hotelSearchRequest) {

        Page<HotelPriceDto> hotel = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(hotel);
    }

    @GetMapping
    public ResponseEntity<Page<HotelDto>> browseHotels(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseEntity.ok(hotelService.getActiveHotels(page, size));
    }

    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfo(@PathVariable Long hotelId){
        return ResponseEntity.ok(hotelService.getHotelInfo(hotelId));
    }

    @GetMapping("/{hotelId}/rooms/{roomId}/quote")
    public ResponseEntity<BookingQuoteDto> getRoomQuote(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @RequestParam LocalDate checkInDate,
            @RequestParam LocalDate checkOutDate,
            @RequestParam Integer roomsCount) {
        BookingRequest request = new BookingRequest();
        request.setHotelId(hotelId);
        request.setRoomId(roomId);
        request.setCheckInDate(checkInDate);
        request.setCheckOutDate(checkOutDate);
        request.setRoomsCount(roomsCount);
        return ResponseEntity.ok(bookingService.getBookingQuote(request));
    }


}
