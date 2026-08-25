package com.example.airbnbBackend.strategy;

import com.example.airbnbBackend.entity.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class OccupancyStrategy implements PricingStrategy{

    private final PricingStrategy wrapped;

    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);

        double occupancyRate = (double) inventory.getBookedCount() /inventory.getTotalCount();

        if(occupancyRate >= 0.8){
            price = price.multiply(BigDecimal.valueOf(1.25));
        }
        return price;
    }
}
