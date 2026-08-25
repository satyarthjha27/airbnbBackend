package com.example.airbnbBackend.services;

import com.example.airbnbBackend.entity.Booking;

public interface PaymentService {

    String checkoutPayemnt(Booking booking, String successUrl, String failureUrl);
}
