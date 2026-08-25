package com.example.airbnbBackend.services;

import com.example.airbnbBackend.entity.User;

public interface UserService {
    User getUserById(Long id);
}
