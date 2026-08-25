package com.example.airbnbBackend.services;

import com.example.airbnbBackend.dto.HotelDto;
import com.example.airbnbBackend.dto.HotelInfoDto;
import com.example.airbnbBackend.dto.RoomDto;
import com.example.airbnbBackend.entity.Hotel;
import com.example.airbnbBackend.entity.Room;
import com.example.airbnbBackend.entity.User;
import com.example.airbnbBackend.exception.ResourceNotFoundException;
import com.example.airbnbBackend.exception.UnAuthorisedException;
import com.example.airbnbBackend.repository.HotelRepository;
import com.example.airbnbBackend.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;
    private final RoomService roomService;
    private final RoomRepository roomRepository;

    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("Creating new hotel with name: {}", hotelDto.getName());
        Hotel hotel = modelMapper.map(hotelDto, Hotel.class);
        hotel.setActive(false);

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        if(!user.equals(hotel.getOwner())){
//            throw new UnAuthorisedException("User Not allowed for this Operation.");
//        }
        hotel.setOwner(user);

        hotel= hotelRepository.save(hotel);
        log.info("Hotel created with id: {}", hotel.getId());
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto getHotelById(Long id) {
        log.info("Fetching Hotel with id: {}", id);
        Hotel hotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: " + id));
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto updateHotel(Long id, HotelDto hotelDto) {
        log.info("Updating Hotel with id: {}", id);
        Hotel existingHotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: " + id));

        modelMapper.map(hotelDto, existingHotel);
        existingHotel.setId(id);
        Hotel updatedHotel = hotelRepository.save(existingHotel);
        log.info("Successfully updated Hotel with id: {}", id);
        return modelMapper.map(updatedHotel, HotelDto.class);
    }

    @Override
    @Transactional
    public void deleteHotel(Long id) {
        log.info("Deleting Hotel with id: {}", id);
        Hotel existingHotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: " + id));

        for(Room room : existingHotel.getRooms()){
            inventoryService.deleteAllInventories(room);
            roomService.deleteRoomByID(room.getId());
        }
        hotelRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void activateHotel(Long id) {
        log.info("Activating Hotel with id: {}", id);
        Hotel existingHotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: " + id));
        existingHotel.setActive(true);
        hotelRepository.save(existingHotel);
        for(Room room : existingHotel.getRooms()){
            inventoryService.initializeRoomsForAYear(room);
        }
    }

    @Override
    public HotelInfoDto getHotelInfo(Long hotelId) {
        Hotel existingHotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: " + hotelId));
        List<RoomDto> rooms = existingHotel.getRooms()
                .stream()
                .map((room)-> modelMapper.map(room,RoomDto.class))
                .toList();

        return new HotelInfoDto(modelMapper.map(existingHotel,HotelDto.class),rooms);
    }

    @Override
    public List<HotelDto> getAllHotels() {
        log.info("Getting all hotels");

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<Hotel> hotels = hotelRepository.findByOwner(user);

        return hotels.stream()
            .map((element) -> modelMapper.map(element, HotelDto.class))
                .collect(Collectors.toList());

    }


}
