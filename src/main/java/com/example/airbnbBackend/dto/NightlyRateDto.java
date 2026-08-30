package com.example.airbnbBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class NightlyRateDto {
    private LocalDate date;
    private BigDecimal rate;
}
