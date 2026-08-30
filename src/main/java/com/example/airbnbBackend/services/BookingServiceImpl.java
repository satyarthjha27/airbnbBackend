package com.example.airbnbBackend.services;

import com.example.airbnbBackend.dto.BookingDto;
import com.example.airbnbBackend.dto.BookingQuoteDto;
import com.example.airbnbBackend.dto.BookingRequest;
import com.example.airbnbBackend.dto.GuestBookingDto;
import com.example.airbnbBackend.dto.GuestDto;
import com.example.airbnbBackend.dto.NightlyRateDto;
import com.example.airbnbBackend.entity.Booking;
import com.example.airbnbBackend.entity.Guest;
import com.example.airbnbBackend.entity.Hotel;
import com.example.airbnbBackend.entity.Inventory;
import com.example.airbnbBackend.entity.Room;
import com.example.airbnbBackend.entity.User;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
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
        validateBookingRequest(bookingRequest);
        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + bookingRequest.getHotelId()));
        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + bookingRequest.getRoomId()));
        validateBookableRoom(hotel, room);

        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(
                room.getId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());
        long nights = numberOfNights(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());
        requireCompleteAvailability(inventoryList, nights);

        inventoryList.forEach(inventory -> inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount()));
        inventoryRepository.saveAll(inventoryList);

        BigDecimal amount = calculateBookingAmount(inventoryList, bookingRequest.getRoomsCount());
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .user(getCurrentUser())
                .room(room)
                .hotel(hotel)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .amount(amount)
                .roomsCount(bookingRequest.getRoomsCount())
                .guests(new HashSet<>())
                .build();

        bookingRepository.save(booking);
        return modelMapper.map(booking, BookingDto.class);
    }

    @Override
    @Transactional
    public BookingQuoteDto getBookingQuote(BookingRequest bookingRequest) {
        validateBookingRequest(bookingRequest);
        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + bookingRequest.getHotelId()));
        Room room = roomRepository.findById(bookingRequest.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + bookingRequest.getRoomId()));
        validateBookableRoom(hotel, room);
        List<Inventory> inventoryList = inventoryRepository.findAvailableInventory(
                room.getId(), bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate(), bookingRequest.getRoomsCount());
        long nights = numberOfNights(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());
        requireCompleteAvailability(inventoryList, nights);

        List<NightlyRateDto> nightlyRates = inventoryList.stream()
                .sorted((left, right) -> left.getDate().compareTo(right.getDate()))
                .map(inventory -> new NightlyRateDto(inventory.getDate(), dynamicNightlyRate(inventory)))
                .toList();
        return BookingQuoteDto.builder()
                .hotelId(hotel.getId())
                .roomId(room.getId())
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .roomsCount(bookingRequest.getRoomsCount())
                .nights(nights)
                .currency("INR")
                .nightlyRates(nightlyRates)
                .total(calculateBookingAmount(inventoryList, bookingRequest.getRoomsCount()))
                .quoteExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    @Override
    @Transactional
    public BookingDto addGuest(Long bookingId, List<GuestDto> guestDtos) {
        Booking booking = getOwnedBooking(bookingId);
        expireIfNecessary(booking);
        if (booking.getBookingStatus() != BookingStatus.RESERVED && booking.getBookingStatus() != BookingStatus.GUEST_ADDED) {
            throw new IllegalStateException("Guest details can only be added while the room is reserved.");
        }
        if (guestDtos == null || guestDtos.isEmpty()) {
            throw new IllegalArgumentException("Add at least one guest before checkout.");
        }

        User user = getCurrentUser();
        if (booking.getGuests() == null) {
            booking.setGuests(new HashSet<>());
        }
        Integer roomCapacity = booking.getRoom().getCapacity();
        if (roomCapacity == null || roomCapacity < 1) {
            throw new IllegalStateException("This room does not have a valid guest capacity.");
        }
        int maximumGuests;
        try {
            maximumGuests = Math.multiplyExact(roomCapacity, booking.getRoomsCount());
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("The selected room count is too large.");
        }
        int guestCountAfterAdding = booking.getGuests().size() + guestDtos.size();
        if (guestCountAfterAdding > maximumGuests) {
            throw new IllegalArgumentException("This booking can include up to " + maximumGuests + " guests.");
        }
        for (GuestDto guestDto : guestDtos) {
            if (guestDto.getName() == null || guestDto.getName().isBlank() || guestDto.getAge() == null || guestDto.getAge() < 1) {
                throw new IllegalArgumentException("Each guest needs a name and valid age.");
            }
            Guest guest = modelMapper.map(guestDto, Guest.class);
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
        Booking booking = getOwnedBooking(bookingId);
        expireIfNecessary(booking);
        if (booking.getBookingStatus() == BookingStatus.PAYMENT_PENDING && booking.getPaymentSessionId() != null) {
            return paymentService.getCheckoutUrl(booking.getPaymentSessionId());
        }
        if (booking.getBookingStatus() != BookingStatus.GUEST_ADDED) {
            throw new IllegalStateException("Add guest details before starting checkout.");
        }

        String baseUrl = frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";
        String sessionUrl = paymentService.checkoutPayemnt(
                booking,
                baseUrl + "bookings/" + bookingId + "?payment=success",
                baseUrl + "bookings/" + bookingId + "?payment=cancelled"
        );
        booking.setBookingStatus(BookingStatus.PAYMENT_PENDING);
        bookingRepository.save(booking);
        return sessionUrl;
    }

    @Override
    @Transactional
    public void capturePayment(Event event) {
        if (!"checkout.session.completed".equals(event.getType())
                && !"checkout.session.async_payment_succeeded".equals(event.getType())) {
            log.warn("Unhandled event type: {}", event.getType());
            return;
        }
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) {
            return;
        }
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            log.info("Checkout session {} is not paid yet; booking confirmation is deferred.", session.getId());
            return;
        }
        Booking booking = bookingRepository.findAndLockByPaymentSessionId(session.getId()).orElseThrow(() ->
                new ResourceNotFoundException("Booking not found for session ID: " + session.getId()));
        confirmPaidBooking(booking);
    }

    private void confirmPaidBooking(Booking booking) {
        if (booking.getBookingStatus() == BookingStatus.CANCELLED || booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            log.info("Ignoring duplicate or cancelled payment event for booking ID: {}", booking.getId());
            return;
        }

        if (booking.getBookingStatus() != BookingStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("This booking is not awaiting payment confirmation.");
        }

        long dayCount = numberOfNights(booking.getCheckInDate(), booking.getCheckOutDate());
        List<Inventory> reservedInventory = inventoryRepository.findAndLockReservedInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());
        if (reservedInventory.size() != dayCount) {
            throw new IllegalStateException("Reserved inventory is no longer available for payment confirmation.");
        }
        int confirmedDays = inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());
        if (confirmedDays != dayCount) {
            throw new IllegalStateException("Payment could not be applied to all reserved dates.");
        }
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        log.info("Successfully confirmed booking ID: {}", booking.getId());
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = getOwnedBooking(bookingId);
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            return;
        }

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            refundConfirmedBooking(booking);
            inventoryRepository.releaseConfirmedBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());
        } else {
            inventoryRepository.releaseReservation(booking.getRoom().getId(), booking.getCheckInDate(),
                    booking.getCheckOutDate(), booking.getRoomsCount());
        }
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public String getBookingStatus(Long bookingId) {
        Booking booking = getOwnedBooking(bookingId);
        expireIfNecessary(booking);
        reconcilePaidCheckout(booking);
        return booking.getBookingStatus().name();
    }

    @Override
    @Transactional
    public Page<GuestBookingDto> getMyBookings(BookingStatus status, Pageable pageable) {
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), 50);
        Pageable safePageable = PageRequest.of(Math.max(pageable.getPageNumber(), 0), pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        User user = getCurrentUser();
        Page<Booking> bookings = status == null
                ? bookingRepository.findByUserOrderByCreatedAtDesc(user, safePageable)
                : bookingRepository.findByUserAndBookingStatusOrderByCreatedAtDesc(user, status, safePageable);
        return bookings.map(this::toGuestBookingDto);
    }

    @Override
    public List<BookingDto> getAllBookingOfHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + hotelId));
        if (!getCurrentUser().equals(hotel.getOwner())) {
            throw new UnAuthorisedException("User not allowed to view these bookings.");
        }
        return bookingRepository.findByHotel(hotel).stream()
                .map(booking -> modelMapper.map(booking, BookingDto.class))
                .collect(Collectors.toList());
    }

    private Booking getOwnedBooking(Long bookingId) {
        Booking booking = bookingRepository.findAndLockById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
        if (!getCurrentUser().equals(booking.getUser())) {
            throw new UnAuthorisedException("User not allowed to edit this booking.");
        }
        return booking;
    }

    private void validateBookingRequest(BookingRequest request) {
        if (request.getHotelId() == null || request.getRoomId() == null || request.getRoomsCount() == null
                || request.getRoomsCount() < 1 || request.getCheckInDate() == null || request.getCheckOutDate() == null
                || request.getCheckInDate().isBefore(LocalDate.now())
                || !request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new IllegalArgumentException("Choose a hotel, room, valid dates, and at least one room.");
        }
    }

    private void validateBookableRoom(Hotel hotel, Room room) {
        if (!room.getHotel().getId().equals(hotel.getId())) {
            throw new IllegalArgumentException("The selected room does not belong to this hotel.");
        }
        if (!Boolean.TRUE.equals(hotel.getActive())) {
            throw new IllegalStateException("This hotel is not accepting bookings yet.");
        }
    }

    private long numberOfNights(LocalDate checkInDate, LocalDate checkOutDate) {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    private void requireCompleteAvailability(List<Inventory> inventoryList, long nights) {
        if (inventoryList.size() != nights) {
            throw new IllegalStateException("This room is no longer available for all selected nights.");
        }
    }

    private BigDecimal dynamicNightlyRate(Inventory inventory) {
        return pricingService.calculateDynamicPricing(inventory).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBookingAmount(List<Inventory> inventoryList, int roomsCount) {
        return inventoryList.stream()
                .map(this::dynamicNightlyRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(roomsCount))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void expireIfNecessary(Booking booking) {
        boolean canExpire = booking.getBookingStatus() == BookingStatus.RESERVED
                || booking.getBookingStatus() == BookingStatus.GUEST_ADDED;
        if (!canExpire || !hasBookingExpired(booking)) {
            return;
        }
        inventoryRepository.releaseReservation(booking.getRoom().getId(), booking.getCheckInDate(),
                booking.getCheckOutDate(), booking.getRoomsCount());
        booking.setBookingStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);
        throw new IllegalStateException("Booking has expired. Please start again.");
    }

    private void refundConfirmedBooking(Booking booking) {
        if (booking.getPaymentSessionId() == null) {
            throw new IllegalStateException("A confirmed booking is missing its payment session.");
        }
        try {
            Session session = Session.retrieve(booking.getPaymentSessionId());
            if (session.getPaymentIntent() == null) {
                throw new IllegalStateException("The payment provider has not supplied a refundable payment intent.");
            }
            Refund.create(RefundCreateParams.builder().setPaymentIntent(session.getPaymentIntent()).build());
        } catch (StripeException exception) {
            throw new RuntimeException("The refund could not be completed. The booking remains active.", exception);
        }
    }

    private void reconcilePaidCheckout(Booking booking) {
        if (booking.getBookingStatus() == BookingStatus.PAYMENT_PENDING && booking.getPaymentSessionId() != null
                && paymentService.isCheckoutPaid(booking.getPaymentSessionId())) {
            confirmPaidBooking(booking);
        }
    }

    private GuestBookingDto toGuestBookingDto(Booking booking) {
        Set<GuestDto> guests = booking.getGuests() == null ? Set.of() : booking.getGuests().stream()
                .map(this::toGuestDto)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String[] hotelPhotos = booking.getHotel().getPhotos();
        String hotelPhoto = hotelPhotos != null && hotelPhotos.length > 0 ? hotelPhotos[0] : null;
        int guestCapacity = Math.multiplyExact(booking.getRoom().getCapacity(), booking.getRoomsCount());
        return GuestBookingDto.builder()
                .id(booking.getId())
                .roomsCount(booking.getRoomsCount())
                .guestCapacity(guestCapacity)
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .createdAt(booking.getCreatedAt())
                .bookingStatus(booking.getBookingStatus())
                .amount(booking.getAmount())
                .hotelId(booking.getHotel().getId())
                .hotelName(booking.getHotel().getName())
                .hotelCity(booking.getHotel().getCity())
                .hotelPhoto(hotelPhoto)
                .roomId(booking.getRoom().getId())
                .roomType(booking.getRoom().getType())
                .guests(guests)
                .build();
    }

    private GuestDto toGuestDto(Guest guest) {
        GuestDto dto = new GuestDto();
        dto.setId(guest.getId());
        dto.setName(guest.getName());
        dto.setAge(guest.getAge());
        dto.setGender(guest.getGender());
        return dto;
    }

    public Boolean hasBookingExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    public User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
