package com.example.airbnbBackend.services;

import com.example.airbnbBackend.dto.HotelDto;
import com.example.airbnbBackend.entity.Hotel;
import com.example.airbnbBackend.repository.HotelMinPriceRepository;
import com.example.airbnbBackend.repository.HotelRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HotelServiceImplTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private RoomService roomService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelMinPriceRepository hotelMinPriceRepository;

    @InjectMocks
    private HotelServiceImpl hotelService;

    @Test
    void getActiveHotelsReturnsOnlyRepositoryResultsAndClampsPagination() {
        Hotel hotel = new Hotel();
        hotel.setId(42L);
        hotel.setName("Active stay");
        hotel.setActive(true);
        HotelDto hotelDto = new HotelDto();
        hotelDto.setId(42L);
        hotelDto.setName("Active stay");
        hotelDto.setActive(true);
        PageRequest expectedPage = PageRequest.of(0, 50);

        when(hotelRepository.findByActiveTrue(expectedPage))
                .thenReturn(new PageImpl<>(List.of(hotel), expectedPage, 1));
        when(modelMapper.map(hotel, HotelDto.class)).thenReturn(hotelDto);

        Page<HotelDto> result = hotelService.getActiveHotels(-3, 1000);

        assertThat(result.getContent()).containsExactly(hotelDto);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(hotelRepository).findByActiveTrue(expectedPage);
    }
}
