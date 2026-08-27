package com.example.airbnbBackend.services;

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
public class RoomServiceImpl implements RoomService{

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public RoomDto createNewRoom(Long hotelId, RoomDto roomDto) {
        log.info("Creating New Room in  Hotel with id: {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: " + hotelId));
        requireOwner(hotel);

        Room room = modelMapper.map(roomDto, Room.class);
        room.setHotel(hotel);
        roomRepository.save(room);
        log.info("Room created with id: {}", room.getId());
        if(hotel.getActive()){
            inventoryService.initializeRoomsForAYear(room);
        }
        return modelMapper.map(room,RoomDto.class);

    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Fetching All Rooms in  Hotel with id: {}", hotelId);
        Hotel hotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: " + hotelId));
        requireOwner(hotel);

        return hotel.getRooms()
                .stream()
                .map((element) -> modelMapper.map(element,RoomDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        log.info("Fetching Rooms with id: {}", roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room Not Found with id: " + roomId));
        requireOwner(room.getHotel());
        return modelMapper.map(room,RoomDto.class);
    }

    @Override
    public void deleteRoomByID(Long roomId) {
        log.info("Deleting Rooms with id: {}", roomId);
        Room room = roomRepository
                .findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room Not Found with id: " + roomId));
        requireOwner(room.getHotel());

        inventoryService.deleteAllInventories(room);
        roomRepository.deleteById(roomId);
    }

    private void requireOwner(Hotel hotel) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!user.equals(hotel.getOwner())) {
            throw new UnAuthorisedException("User Not allowed for this Operation.");
        }
    }
}
