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
import com.example.airbnbBackend.repository.HotelMinPriceRepository;
import com.example.airbnbBackend.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final HotelMinPriceRepository hotelMinPriceRepository;

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
        requireOwner(hotel);
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto updateHotel(Long id, HotelDto hotelDto) {
        log.info("Updating Hotel with id: {}", id);
        Hotel existingHotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: " + id));
        requireOwner(existingHotel);

        Boolean active = existingHotel.getActive();
        modelMapper.map(hotelDto, existingHotel);
        existingHotel.setId(id);
        existingHotel.setActive(active);
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
        requireOwner(existingHotel);

        for(Room room : existingHotel.getRooms()){
            inventoryService.deleteAllInventories(room);
            roomService.deleteRoomByID(room.getId());
        }
        hotelMinPriceRepository.deleteByHotel(existingHotel);
        hotelRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void activateHotel(Long id) {
        log.info("Activating Hotel with id: {}", id);
        Hotel existingHotel = hotelRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: " + id));
        requireOwner(existingHotel);
        if (Boolean.TRUE.equals(existingHotel.getActive())) {
            return;
        }
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
        if (!Boolean.TRUE.equals(existingHotel.getActive()) && !isCurrentOwner(existingHotel)) {
            throw new ResourceNotFoundException("Hotel Not Found with id: " + hotelId);
        }
        List<RoomDto> rooms = existingHotel.getRooms()
                .stream()
                .map((room)-> modelMapper.map(room,RoomDto.class))
                .toList();

        return new HotelInfoDto(modelMapper.map(existingHotel,HotelDto.class),rooms);
    }

    @Override
    public Page<HotelDto> getActiveHotels(Integer page, Integer size) {
        int safePage = Math.max(0, page == null ? 0 : page);
        int safeSize = Math.min(50, Math.max(1, size == null ? 10 : size));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return hotelRepository.findByActiveTrue(pageable)
                .map(hotel -> modelMapper.map(hotel, HotelDto.class));
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

    private void requireOwner(Hotel hotel) {
        if (!isCurrentOwner(hotel)) {
            throw new UnAuthorisedException("User Not allowed for this Operation.");
        }
    }

    private boolean isCurrentOwner(Hotel hotel) {
        if (SecurityContextHolder.getContext().getAuthentication() == null
                || !(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User user)) {
            return false;
        }
        return user.equals(hotel.getOwner());
    }


}
