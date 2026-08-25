package com.example.airbnbBackend.controller;

import com.example.airbnbBackend.dto.BookingDto;
import com.example.airbnbBackend.dto.HotelDto;
import com.example.airbnbBackend.services.BookingService;
import com.example.airbnbBackend.services.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/admin/hotels")
public class HotelController {

    private final HotelService hotelService;
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<HotelDto> createNewHotel(@RequestBody HotelDto hotelDto){
        log.info("Attempting to create a new Hotel with name: {}", hotelDto.getName());
        HotelDto createdHotel = hotelService.createNewHotel(hotelDto);
        log.info("Successfully created a new Hotel with name: {}", hotelDto.getName());
        return new ResponseEntity<>(createdHotel,HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HotelDto> getHotelById(@PathVariable Long id){
        log.info("Fetching Hotel with id: {}", id);
        HotelDto hotelDto = hotelService.getHotelById(id);
        return ResponseEntity.ok(hotelDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HotelDto> updateHotel(@PathVariable Long id, @RequestBody HotelDto hotelDto) {
        log.info("Attempting to update Hotel with id: {}", id);
        HotelDto updatedHotel = hotelService.updateHotel(id, hotelDto);
        log.info("Successfully updated Hotel with id: {}", id);
        return ResponseEntity.ok(updatedHotel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> updateHotel(@PathVariable Long id ){
        log.info("Attempting to Delete Hotel with id: {}", id);
        hotelService.deleteHotel(id);
        log.info("Successfully Deleted Hotel with id: {}", id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> activateHotel(@PathVariable Long id){
        log.info("Attempting to Activate Hotel with id: {}", id);
        hotelService.activateHotel(id);
        log.info("Successfully Activated Hotel with id: {}", id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<HotelDto>> getAllHotels(){
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    @GetMapping("/{hotelId}/booking")
    public ResponseEntity<List<BookingDto>> getAllBookingOfHotel(@PathVariable Long hotelId){
        return ResponseEntity.ok(bookingService.getAllBookingOfHotel(hotelId));
    }


}
