package com.example.airbnbBackend.repository;

import com.example.airbnbBackend.dto.HotelPriceProjection;
import com.example.airbnbBackend.entity.Hotel;
import com.example.airbnbBackend.entity.HotelMinPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HotelMinPriceRepository extends JpaRepository<HotelMinPrice, Long> {

    @Query("""
            SELECT new com.example.airbnbBackend.dto.HotelPriceProjection(i.hotel, AVG(i.price))
            FROM HotelMinPrice i
            where i.date >= :startDate AND i.date < :endDate
                AND i.hotel.active = true
                AND LOWER(TRIM(i.hotel.city)) = LOWER(TRIM(:city))
                AND i.hotel IN (
                    SELECT available.hotel
                    FROM Inventory available
                    WHERE available.date >= :startDate AND available.date < :endDate
                        AND available.closed = false
                        AND (available.totalCount - available.bookedCount - available.reservedCount) >= :roomsCount
                    GROUP BY available.hotel, available.room
                    HAVING COUNT(available.date) = :dateCount
                )
            GROUP BY i.hotel
            HAVING COUNT(i.date) = :dateCount
            """)
    Page<HotelPriceProjection> findHotelsWithAvailableInventory(
            @Param("city") String city,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("roomsCount") Integer roomsCount,
            @Param("dateCount") Long dateCount,
            Pageable pageable
    );

    Optional<HotelMinPrice> findByHotelAndDate(Hotel hotel, LocalDate date);

    void deleteByHotel(Hotel hotel);
}
