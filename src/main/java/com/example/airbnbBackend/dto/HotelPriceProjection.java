package com.example.airbnbBackend.dto;

import com.example.airbnbBackend.entity.Hotel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal persistence projection. It must be mapped to HotelPriceDto before
 * leaving the service layer so JPA entities are never serialized publicly.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotelPriceProjection {

    private Hotel hotel;
    private Double price;
}
