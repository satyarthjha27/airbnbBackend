package com.example.airbnbBackend.dto;

import com.example.airbnbBackend.entity.Booking;
import com.example.airbnbBackend.entity.User;
import com.example.airbnbBackend.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Data
public class GuestDto {

    private Long id;
    private User user;
    private String name;
    private Gender gender;
    private Integer age;

}
