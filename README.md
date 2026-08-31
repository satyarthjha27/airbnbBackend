# Airbnb Backend

Spring Boot REST API for an Airbnb-style booking application. It supports user authentication, public hotel discovery, date-based room availability, hotel management, bookings, Stripe Checkout, and signed Stripe webhooks.

## Technology stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC and Spring Data JPA
- Spring Security with JWT authentication
- PostgreSQL
- Stripe Checkout and webhooks
- Maven Wrapper
- Swagger UI / OpenAPI

## Requirements

- JDK 21
- PostgreSQL
- Docker (optional)
- Stripe test credentials when testing payments

## Configuration

The application reads its runtime configuration from environment variables.

| Variable | Required | Description |
| --- | --- | --- |
| `DB_URL` | Yes | PostgreSQL JDBC URL, such as `jdbc:postgresql://localhost:5432/airbnb` |
| `DB_USERNAME` | Yes | PostgreSQL username |
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET_KEY` | Yes | Secret used to sign JWTs; use a long, random value |
| `STRIPE_SECRET_KEY` | Yes | Stripe secret API key for the selected Stripe mode |
| `STRIPE_WEBHOOK_SECRET` | Yes | Signing secret for this backend's Stripe webhook endpoint |
| `FRONTEND_URL` | No | Allowed frontend origin and payment redirect base; defaults to `http://localhost:3000/` |
| `PORT` | No | HTTP port; defaults to `8080` |

For a local shell session, set the values before starting the application:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/airbnb'
export DB_USERNAME='postgres'
export DB_PASSWORD='replace-with-your-password'
export JWT_SECRET_KEY='replace-with-a-long-random-secret'
export STRIPE_SECRET_KEY='sk_test_replace_me'
export STRIPE_WEBHOOK_SECRET='whsec_replace_me'
export FRONTEND_URL='http://localhost:3000/'
```

Never commit real database, JWT, or Stripe secrets. If a real secret has previously been committed, rotate it in the corresponding service.

## Run locally

Start PostgreSQL and create the database referenced by `DB_URL`, then run:

```bash
./mvnw spring-boot:run
```

The API will be available at:

```text
http://localhost:8080/api/v1
```

Hibernate currently uses `ddl-auto: update`, so the database schema is created or updated when the application starts.

## API documentation

With the application running:

- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v1/v3/api-docs`

## Main endpoints

All paths below are relative to `/api/v1`.

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/auth/signup` | Public | Register a guest or hotel manager |
| `POST` | `/auth/login` | Public | Sign in and receive an access token |
| `POST` | `/auth/refresh` | Public cookie flow | Refresh the access token |
| `GET` | `/hotels` | Public | Browse active hotels with pagination |
| `GET` | `/hotels/search` | Public | Search available hotels by city, dates, and room count |
| `GET` | `/hotels/{hotelId}/info` | Public | Get hotel and room details |
| `GET` | `/hotels/{hotelId}/rooms/{roomId}/quote` | Public | Get a non-binding room quote |
| `POST` | `/bookings` | Authenticated | Create a pending booking |
| `GET` | `/bookings/me` | Authenticated | List the current user's bookings |
| `POST` | `/bookings/{bookingId}/payment` | Authenticated | Create a Stripe Checkout session |
| `POST` | `/bookings/{bookingId}/cancel` | Authenticated | Cancel a booking |
| `GET/POST/PUT/PATCH/DELETE` | `/admin/**` | `HOTEL_MANAGER` | Manage hotels, rooms, and hotel bookings |
| `POST` | `/webhook/payment` | Stripe-signed | Receive Stripe payment events |

Protected requests use a bearer access token:

```http
Authorization: Bearer <access-token>
```

### Availability search example

Dates use ISO `YYYY-MM-DD` format. The city comparison is case-insensitive, and a hotel is returned only when one room type can satisfy `roomsCount` for every selected night.

```bash
curl 'http://localhost:8080/api/v1/hotels/search?city=goa&startDate=2026-09-12&endDate=2026-09-15&roomsCount=1&page=0&size=10'
```

### Browse active hotels example

```bash
curl 'http://localhost:8080/api/v1/hotels?page=0&size=10'
```

## Tests and build

Run the test suite:

```bash
./mvnw test
```

Create the executable JAR:

```bash
./mvnw clean package
java -jar target/airbnbBackend-0.0.1-SNAPSHOT.jar
```

## Docker

Build and run the included multi-stage image:

```bash
docker build -t airbnb-backend .
docker run --env-file .env -p 8080:8080 airbnb-backend
```

The `.env` file used by Docker should contain the variables listed in the configuration section and must remain outside version control.

## Stripe webhooks

The public webhook URL is:

```text
<backend-origin>/api/v1/webhook/payment
```

The endpoint verifies the `Stripe-Signature` header before confirming a payment. See [WEBHOOK_SETUP.md](WEBHOOK_SETUP.md) for deployment, Stripe Dashboard, verification, and troubleshooting instructions.

For local Stripe CLI testing:

```bash
stripe listen --forward-to localhost:8080/api/v1/webhook/payment
```

Use the webhook signing secret printed by the Stripe CLI as `STRIPE_WEBHOOK_SECRET` for that local session.

## Project structure

```text
src/main/java/com/example/airbnbBackend/
├── controller/   # REST endpoints
├── dto/          # API request and response models
├── entity/       # JPA entities
├── repository/   # Spring Data repositories and availability queries
├── security/     # JWT authentication and authorization
└── services/     # Business logic and Stripe integration
```
