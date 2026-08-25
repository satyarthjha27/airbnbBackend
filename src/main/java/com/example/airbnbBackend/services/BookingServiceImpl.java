package com.example.airbnbBackend.services;

import com.example.airbnbBackend.dto.BookingDto;
import com.example.airbnbBackend.dto.BookingRequest;
import com.example.airbnbBackend.dto.GuestDto;
import com.example.airbnbBackend.entity.*;
import com.example.airbnbBackend.entity.enums.BookingStatus;
import com.example.airbnbBackend.exception.ResourceNotFoundException;
import com.example.airbnbBackend.exception.UnAuthorisedException;
import com.example.airbnbBackend.repository.BookingRepository;
import com.example.airbnbBackend.repository.HotelRepository;
import com.example.airbnbBackend.repository.InventoryRepository;
import com.example.airbnbBackend.repository.RoomRepository;
import com.example.airbnbBackend.strategy.PricingService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService{
    private final PaymentService paymentService;
    private final GuestRepository guestRepository;
    private final InventoryRepository inventoryRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final PricingService pricingService;

    private final BookingRepository bookingRepository;

    @Value("${frontendUrl}")
    private String frontendUrl;

    @Override
    @Transactional
    public @Nullable BookingDto initialiseBooking(BookingRequest bookingRequest) {

        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(() -> new RuntimeException("Hotel Not found with id: {}" + bookingRequest.getHotelId()));
        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room Not found with id: {}" + bookingRequest.getRoomId()));

        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(room.getId(),bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount());

        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(),bookingRequest.getCheckOutDate()) +1;

        if(inventoryList.size() != daysCount){
            throw new IllegalStateException("Room is not availabe anymore");
        }



        // Reserve the room/ update the reservedCount;

        for(Inventory inventory: inventoryList){
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount());
        }
         inventoryRepository.saveAll(inventoryList);

        //Create Booking in Booking Repository


        BigDecimal amount = pricingService.calculateTotalPrice(inventoryList).multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));

        User user = getCurrentUser();
        Booking booking =Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .user(user)
                .room(room)
                .hotel(hotel)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .amount(amount)
                .roomsCount(bookingRequest.getRoomsCount())
                .build();

        bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    @Override
    public BookingDto addGuest(Long bookingId, List<GuestDto> guestDto) {

        log.info("Adding Guest in booking with id: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking Not Found with id:" + bookingId));

        if(hasBookingExpired(booking)){
            throw new IllegalStateException("Booking Has Expired");
        }

        if(booking.getBookingStatus() != BookingStatus.RESERVED){
            throw new IllegalStateException("Booking is not under reserved state.");
        }

        User user = getCurrentUser();
        if(!user.equals(booking.getUser())){
            throw new UnAuthorisedException("User Not allowed to edit this booking");
        }

        for(GuestDto guestDto1: guestDto){
            Guest guest = modelMapper.map(guestDto1, Guest.class);
            guest.setUser(user);
            guestRepository.save(guest);
            booking.getGuests().add(guest);
        }

        booking.setBookingStatus(BookingStatus.GUEST_ADDED);
        bookingRepository.save(booking);

        return modelMapper.map(booking, BookingDto.class);

    }

    @Override
    @Transactional
    public String initialiseCheckout(Long bookingId) {

        log.info("Initialising Payemnt with booking with id: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking Not Found with id:" + bookingId));

        if(hasBookingExpired(booking)){
            throw new IllegalStateException("Booking Has Expired");
        }

        User user = getCurrentUser();
        if(!user.equals(booking.getUser())){
            throw new UnAuthorisedException("User Not allowed to edit this booking");
        }


        String sessionUrl =  paymentService.checkoutPayemnt(booking,frontendUrl+"payments/success",frontendUrl+"payments/failure");

        booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);
        bookingRepository.save(booking);

        return sessionUrl;
    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            if (session == null) return;

            String sessionId = session.getId();
            Booking booking =
                    bookingRepository.findByPaymentSessionId(sessionId).orElseThrow(() ->
                            new ResourceNotFoundException("Booking not found for session ID: "+sessionId));

            booking.setBookingStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());

            log.info("Successfully confirmed the booking for Booking ID: {}", booking.getId());
        } else {
            log.warn("Unhandled event type: {}", event.getType());
        }
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {

        log.info("Cancelling Payemnt with booking with id: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking Not Found with id:" + bookingId));


        User user = getCurrentUser();
        if(!user.equals(booking.getUser())){
            throw new UnAuthorisedException("User Not allowed to edit this booking");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        inventoryRepository.cancelBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());

        try{
            Session session = Session.retrieve(booking.getPaymentSessionId());
            RefundCreateParams refundCreateParams = RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .build();
            Refund.create(refundCreateParams);
        } catch (StripeException e){
            throw new RuntimeException("Issue in Refund in stripe" + e);
        }

    }

    @Override
    public String getBookingStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking Not Found with id:" + bookingId));

        User user = getCurrentUser();
        if(!user.equals(booking.getUser())){
            throw new UnAuthorisedException("User Not allowed to edit this booking");
        }

        return booking.getBookingStatus().name();
    }

    @Override
    public List<BookingDto> getAllBookingOfHotel(Long hotelId) {

        Hotel existingHotel = hotelRepository
                .findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel Not Found with id: " + hotelId));

        User user = getCurrentUser();
        if(!user.equals(existingHotel.getOwner())){
            throw new UnAuthorisedException("User Not allowed to edit this booking");
        }

        List<Booking> booking = bookingRepository.findByHotel(existingHotel);

        return booking.stream()
                .map((element) -> modelMapper.map(element, BookingDto.class))
                .collect(Collectors.toList());
    }

    public Boolean hasBookingExpired(Booking booking){
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    public User getCurrentUser(){
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
