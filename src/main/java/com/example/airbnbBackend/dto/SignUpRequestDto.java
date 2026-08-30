package com.example.airbnbBackend.dto;

import com.example.airbnbBackend.entity.enums.Role;
import lombok.Data;

@Data
public class SignUpRequestDto {

    private Long id;
    private String email;
    private String password;
    private String name;
    /**
     * GUEST is the safe default. HOTEL_MANAGER is only submitted by the
     * dedicated host registration flow.
     */
    private Role role;
}
