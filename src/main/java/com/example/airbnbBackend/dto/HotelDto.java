package com.example.airbnbBackend.dto;

import com.example.airbnbBackend.entity.HotelContactInfo;
import lombok.Data;

@Data
public class HotelDto {
    private Long id;
    private String name;
    private String city;
    private String[] photos;
    private String[] amenities;
    private HotelContactInfo contact_info;
    private Boolean active;
}
