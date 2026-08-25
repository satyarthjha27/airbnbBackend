package com.example.airbnbBackend.services;

import com.example.airbnbBackend.entity.Hotel;
import com.example.airbnbBackend.entity.HotelMinPrice;
import com.example.airbnbBackend.entity.Inventory;
import com.example.airbnbBackend.repository.HotelMinPriceRepository;
import com.example.airbnbBackend.repository.HotelRepository;
import com.example.airbnbBackend.repository.InventoryRepository;
import com.example.airbnbBackend.strategy.PricingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PricingUpdateService {

    private final InventoryRepository inventoryRepository;
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final HotelRepository hotelRepository;
    private final PricingService pricingService;


    //@Scheduled(cron = "*/5 * * * * *")
    @Scheduled(cron = "0 */5 * * * *")
    public void updatePrice(){

        int page = 0;
        int batchSize=100;
        while(true) {
            Page<Hotel> hotelPage = hotelRepository.findAll(PageRequest.of(page, batchSize));
            if(hotelPage.isEmpty()){
                break;
            }
            hotelPage.getContent().forEach(this::updateHotelPrice);
            page++;
        }

    }

    private void updateHotelPrice(Hotel hotel){
        log.info("Updating Hotel Price of Hotel with id: {}", hotel.getId());

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);

        List<Inventory> inventoryList = inventoryRepository.findByHotelAndDateBetween(hotel,startDate,endDate);

        updateInventoryPrice(inventoryList);

        updateMinHotelPrice(hotel,inventoryList,startDate,endDate);

    }

    private void updateInventoryPrice(List<Inventory> inventoryList){
        inventoryList.forEach((inventory -> {
            BigDecimal dynamicPrice = pricingService.calculateDynamicPricing(inventory);
            inventory.setPrice(dynamicPrice);
        }));

        inventoryRepository.saveAll(inventoryList);
    }

    private void updateMinHotelPrice(Hotel hotel, List<Inventory> inventoryList, LocalDate startDate, LocalDate endDate){

        Map<LocalDate, BigDecimal> map = inventoryList.stream()
                .collect(Collectors.groupingBy(
                        Inventory::getDate,
                        Collectors.mapping(Inventory::getPrice, Collectors.minBy(Comparator.naturalOrder()))
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().orElse(BigDecimal.ZERO)));

        List<HotelMinPrice> hotelMinPrices = new ArrayList<>();
        map.forEach((date,price) -> {
                    HotelMinPrice hotelPrice = hotelMinPriceRepository.findByHotelAndDate(hotel, date)
                            .orElse(new HotelMinPrice(hotel,date));
                    hotelPrice.setPrice(price);
                    hotelMinPrices.add(hotelPrice);
                }
        );
        hotelMinPriceRepository.saveAll(hotelMinPrices);
    }
}
