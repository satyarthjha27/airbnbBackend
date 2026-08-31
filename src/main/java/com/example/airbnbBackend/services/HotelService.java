package com.example.airbnbBackend.services;

import com.example.airbnbBackend.dto.HotelDto;
import com.example.airbnbBackend.dto.HotelInfoDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface HotelService {

    HotelDto createNewHotel(HotelDto hotelDto);

    HotelDto getHotelById(Long id);

    HotelDto updateHotel(Long id, HotelDto hotelDto);

    void deleteHotel(Long id);

    void activateHotel(Long id);


    HotelInfoDto getHotelInfo(Long hotelId);

    Page<HotelDto> getActiveHotels(Integer page, Integer size);

    List<HotelDto> getAllHotels();
}
