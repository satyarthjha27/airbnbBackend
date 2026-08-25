package com.example.airbnbBackend.services;

import com.example.airbnbBackend.dto.HotelDto;
import com.example.airbnbBackend.dto.HotelPriceDto;
import com.example.airbnbBackend.dto.HotelSearchRequest;
import com.example.airbnbBackend.entity.Room;
import org.springframework.data.domain.Page;

public interface InventoryService {
    void initializeRoomsForAYear(Room room);

    void deleteAllInventories(Room room);

    Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest);
}
