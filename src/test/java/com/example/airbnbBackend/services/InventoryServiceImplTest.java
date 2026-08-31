package com.example.airbnbBackend.services;

import com.example.airbnbBackend.dto.HotelDto;
import com.example.airbnbBackend.dto.HotelPriceDto;
import com.example.airbnbBackend.dto.HotelPriceProjection;
import com.example.airbnbBackend.dto.HotelSearchRequest;
import com.example.airbnbBackend.entity.Hotel;
import com.example.airbnbBackend.repository.HotelMinPriceRepository;
import com.example.airbnbBackend.repository.InventoryRepository;
import com.example.airbnbBackend.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private HotelMinPriceRepository hotelMinPriceRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void searchHotelsTrimsCityPreservesRoomCountAndReturnsSafeHotelDto() {
        LocalDate startDate = LocalDate.of(2026, 9, 12);
        LocalDate endDate = LocalDate.of(2026, 9, 15);
        HotelSearchRequest request = new HotelSearchRequest("  goa  ", startDate, endDate, 2, 0, 9);
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("The Blue Fig House");
        HotelDto safeHotel = new HotelDto();
        safeHotel.setId(1L);
        safeHotel.setName("The Blue Fig House");
        PageRequest pageRequest = PageRequest.of(0, 9);
        HotelPriceProjection projection = new HotelPriceProjection(hotel, 7920.0);

        when(hotelMinPriceRepository.findHotelsWithAvailableInventory(
                eq("goa"), eq(startDate), eq(endDate), eq(2), eq(3L), eq(pageRequest)))
                .thenReturn(new PageImpl<>(List.of(projection), pageRequest, 1));
        when(modelMapper.map(hotel, HotelDto.class)).thenReturn(safeHotel);

        Page<HotelPriceDto> result = inventoryService.searchHotels(request);

        assertThat(result.getContent()).containsExactly(new HotelPriceDto(safeHotel, 7920.0));
        verify(hotelMinPriceRepository).findHotelsWithAvailableInventory(
                "goa", startDate, endDate, 2, 3L, pageRequest);
    }
}
