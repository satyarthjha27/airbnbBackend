package com.example.airbnbBackend.repository;

import com.example.airbnbBackend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
