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
        // A holiday surcharge needs an approved, real holiday calendar. Until one exists,
        // never silently charge every stay a holiday uplift.
        return wrapped.calculatePrice(inventory);
    }
}
