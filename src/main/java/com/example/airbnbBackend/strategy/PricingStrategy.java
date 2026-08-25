package com.example.airbnbBackend.strategy;

import com.example.airbnbBackend.entity.Inventory;

import java.math.BigDecimal;

public interface PricingStrategy {

    BigDecimal calculatePrice(Inventory inventory);
}
