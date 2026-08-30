package com.example.airbnbBackend.services;

import com.example.airbnbBackend.dto.HotelDto;
import com.example.airbnbBackend.dto.HotelPriceDto;
import com.example.airbnbBackend.dto.HotelSearchRequest;
import com.example.airbnbBackend.entity.Hotel;
import com.example.airbnbBackend.entity.Inventory;
import com.example.airbnbBackend.entity.Room;
import com.example.airbnbBackend.exception.ResourceNotFoundException;
import com.example.airbnbBackend.repository.HotelMinPriceRepository;
import com.example.airbnbBackend.repository.InventoryRepository;
import com.example.airbnbBackend.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final InventoryRepository inventoryRepository;
    private final RoomRepository roomRepository;

    private final ModelMapper modelMapper;

    @Override
    public void initializeRoomsForAYear(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        log.info("Initializing inventory for Room with id: {} from {} to {}", room.getId(), today, endDate);
        // Logic to initialize inventory for the room for the next year

        for(;!today.isAfter(endDate); today = today.plusDays(1)) {
            Inventory inventory = Inventory.builder()
                    .room(room)
                    .hotel(room.getHotel())
                    .date(today)
                    .totalCount(room.getTotalCount())
                    .surgeFactor(BigDecimal.ONE)
                    .price(room.getBasePrice())
                    .city(room.getHotel().getCity())
                    .closed(false)
                    .bookedCount(0)
                    .reservedCount(0)
                    .build();
            inventoryRepository.save(inventory);
        }

    }

    @Override
    public void deleteAllInventories(Room room) {
        inventoryRepository.deleteByRoom(room);
    }

    @Override
    public Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest) {
        int page = Math.max(0, hotelSearchRequest.getPage() == null ? 0 : hotelSearchRequest.getPage());
        int size = Math.min(50, Math.max(1, hotelSearchRequest.getSize() == null ? 10 : hotelSearchRequest.getSize()));
        Pageable pageable = PageRequest.of(page, size);
        if (hotelSearchRequest.getCity() == null || hotelSearchRequest.getCity().isBlank()
                || hotelSearchRequest.getStartDate() == null || hotelSearchRequest.getEndDate() == null
                || !hotelSearchRequest.getEndDate().isAfter(hotelSearchRequest.getStartDate())
                || hotelSearchRequest.getRoomsCount() == null || hotelSearchRequest.getRoomsCount() < 1) {
            return Page.empty(pageable);
        }
        long dateCount = ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate());


        Page<HotelPriceDto> hotel1= hotelMinPriceRepository.findHotelsWithAvailableInventory(hotelSearchRequest.getCity(),hotelSearchRequest.getStartDate(),hotelSearchRequest.getEndDate(),
                hotelSearchRequest.getRoomsCount(),dateCount, pageable);

        //return hotel1.map((element) -> modelMapper.map(element,HotelDto.class));
        return hotel1;
    }


}
