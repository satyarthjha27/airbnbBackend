package com.example.airbnbBackend.controller;


import com.example.airbnbBackend.dto.HotelDto;
import com.example.airbnbBackend.dto.HotelInfoDto;
import com.example.airbnbBackend.dto.HotelPriceDto;
import com.example.airbnbBackend.dto.HotelSearchRequest;
import com.example.airbnbBackend.services.HotelService;
import com.example.airbnbBackend.services.InventoryService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/hotels")
public class HotelBrowseContoller {

    private final InventoryService inventoryService;
    private final HotelService hotelService;

    @GetMapping("/search")
    public ResponseEntity<Page<HotelPriceDto>> searchHotels(@RequestBody HotelSearchRequest hotelSearchRequest) {

        Page<HotelPriceDto> hotel = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(hotel);
    }

    @GetMapping("/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfo(@PathVariable Long hotelId){
        return ResponseEntity.ok(hotelService.getHotelInfo(hotelId));
    }


}
