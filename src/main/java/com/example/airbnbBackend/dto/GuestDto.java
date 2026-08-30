package com.example.airbnbBackend.dto;

import com.example.airbnbBackend.entity.enums.Gender;
import lombok.Data;

@Data
public class GuestDto {

    private Long id;
    private String name;
    private Gender gender;
    private Integer age;

}
