# Stripe webhook setup on Render

The backend confirms a payment only after it receives a Stripe-signed event or, for an authenticated returning customer, after it securely verifies the Checkout session with Stripe. A frontend `?payment=success` URL never changes the database by itself.

## 1. Deploy the backend changes

Deploy this backend repository to the Render web service serving:

```text
https://airbnbbackend-5yh5.onrender.com
```

After deployment, the public webhook URL is:

```text
https://airbnbbackend-5yh5.onrender.com/api/v1/webhook/payment
```

The endpoint must remain public. It validates Stripe's `Stripe-Signature` header itself; do not add a browser JWT requirement to this route.

## 2. Configure Render environment variables

In the Render service’s environment settings, set the values for the same Stripe mode:

```text
STRIPE_SECRET_KEY=<Stripe test or live secret key>
STRIPE_WEBHOOK_SECRET=<signing secret for this exact webhook endpoint>
FRONTEND_URL=<deployed frontend origin>
```

`STRIPE_WEBHOOK_SECRET` is not the Stripe API key. Copy it from the webhook endpoint’s signing-secret panel in Stripe. Keep all three values in Render’s secret environment variables, never in source control or `NEXT_PUBLIC_*` frontend variables.

The existing `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET_KEY` must also be present for the service to start.

## 3. Register the Stripe endpoint

In the Stripe Dashboard for the same mode as `STRIPE_SECRET_KEY`:

1. Create a webhook endpoint with the URL above.
2. Subscribe to `checkout.session.completed`.
3. Also subscribe to `checkout.session.async_payment_succeeded` if delayed payment methods will be enabled.
4. Copy that endpoint’s signing secret into Render as `STRIPE_WEBHOOK_SECRET` and redeploy or restart the service.

## 4. Verify a real test flow

1. Create a reservation through the frontend and complete Stripe Checkout in test mode.
2. In Stripe, confirm the webhook delivery returned an HTTP `204`.
3. In Render logs, look for `Successfully confirmed booking ID`.
4. Open the booking status page or My bookings; it should show `CONFIRMED`.
5. Confirm inventory changed from reserved to booked in the database.

Do not use a generic Stripe “send test event” with a fabricated session ID as the final test: this backend correctly rejects it because no local booking owns that session. A completed Checkout session created by this backend is the correct end-to-end test.

## Troubleshooting

- **401/403:** the webhook URL is incorrect or a proxy rule is protecting it. Stripe does not send your application JWT.
- **400/500 with signature error:** `STRIPE_WEBHOOK_SECRET` does not match this Stripe endpoint or mode. Update it and restart Render.
- **404:** ensure the `/api/v1` context path is included in the Stripe URL.
- **No delivery:** verify the Render service is public and healthy, and inspect Stripe’s webhook delivery log.
- **Payment stays pending:** verify Stripe marked the Checkout session `paid`, then inspect Render logs and the webhook delivery response. The authenticated booking status endpoint can safely reconcile a paid session while webhook delivery is being fixed, but it should not replace a production webhook.
