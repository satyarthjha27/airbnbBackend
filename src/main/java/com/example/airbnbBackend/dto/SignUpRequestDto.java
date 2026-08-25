package com.example.airbnbBackend.dto;

import lombok.Data;

@Data
public class SignUpRequestDto {

    private Long id;
    private String email;
    private String password;
}
