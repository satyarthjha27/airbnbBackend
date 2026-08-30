package com.example.airbnbBackend.services;

import com.example.airbnbBackend.entity.Booking;

public interface PaymentService {

    String checkoutPayemnt(Booking booking, String successUrl, String failureUrl);

    String getCheckoutUrl(String sessionId);

    /**
     * Reads Stripe server-to-server to reconcile a return from Checkout when a webhook is delayed.
     */
    boolean isCheckoutPaid(String sessionId);
}
