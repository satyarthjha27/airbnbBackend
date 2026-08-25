package com.example.airbnbBackend.services;

import com.example.airbnbBackend.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {
}