package com.example.airbnbBackend.strategy;

import com.example.airbnbBackend.entity.Inventory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PricingService {

    public BigDecimal calculateDynamicPricing(Inventory inventory){
        PricingStrategy pricingStrategy = new BasePriceStrategy();

        //apply additional
        pricingStrategy = new SurgePriceStrategy(pricingStrategy);
        pricingStrategy = new OccupancyStrategy(pricingStrategy);
        pricingStrategy = new UrgencyPricingStrategy(pricingStrategy);
        return pricingStrategy.calculatePrice(inventory);
    }

    public BigDecimal calculateTotalPrice(List<Inventory> inventoryList){
        return inventoryList.stream()
                .map((this::calculateDynamicPricing))
                .reduce(BigDecimal.ZERO,BigDecimal::add);
    }



}
