package com.example.airbnbBackend.dto;

import com.example.airbnbBackend.entity.enums.Role;
import lombok.Data;

import java.util.Set;

@Data
public class UserDto {

    private Long id;
    private String email;
    private String name;
    private Set<Role> roles;
}
