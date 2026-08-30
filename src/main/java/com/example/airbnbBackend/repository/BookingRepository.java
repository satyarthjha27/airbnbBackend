package com.example.airbnbBackend.repository;

import com.example.airbnbBackend.entity.Booking;
import com.example.airbnbBackend.entity.Hotel;
import com.example.airbnbBackend.entity.User;
import com.example.airbnbBackend.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {


    Optional<Booking> findByPaymentSessionId(String sessionId);
    List<Booking> findByHotel(Hotel hotel);
    Page<Booking> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Booking> findByUserAndBookingStatusOrderByCreatedAtDesc(User user, BookingStatus bookingStatus, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT booking FROM Booking booking WHERE booking.id = :bookingId")
    Optional<Booking> findAndLockById(@Param("bookingId") Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT booking FROM Booking booking WHERE booking.paymentSessionId = :sessionId")
    Optional<Booking> findAndLockByPaymentSessionId(@Param("sessionId") String sessionId);

}
