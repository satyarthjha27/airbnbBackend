package com.example.airbnbBackend.strategy;

import com.example.airbnbBackend.entity.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class HolidayPricingStrategy implements PricingStrategy{
    private final PricingStrategy wrapped;
    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
        BigDecimal price = wrapped.calculatePrice(inventory);

        boolean isHoliday = true; // Calculate this on the basis of constant or third party API

        if(isHoliday){
            price = price.multiply(BigDecimal.valueOf(1.1));
        }

        return price;
    }
}
