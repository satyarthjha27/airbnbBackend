package com.example.airbnbBackend.services;

import com.example.airbnbBackend.dto.HotelDto;
import com.example.airbnbBackend.dto.HotelInfoDto;
import com.example.airbnbBackend.entity.Hotel;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface HotelService {

    HotelDto createNewHotel(HotelDto hotelDto);

    HotelDto getHotelById(Long id);

    HotelDto updateHotel(Long id, HotelDto hotelDto);

    void deleteHotel(Long id);

    void activateHotel(Long id);


    HotelInfoDto getHotelInfo(Long hotelId);

    List<HotelDto> getAllHotels();
}
