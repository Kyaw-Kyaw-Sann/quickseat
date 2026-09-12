# QuickSeat API Documentation

Base URL: `http://localhost:8080/api/v1`

All paths below are relative to this Base URL. Public endpoints do not need a token. Protected endpoints need an access token with the role shown under `Access`.

Protected API များအတွက် header:

```text
Authorization: Bearer paste-access-token-here
```

`ADMIN`, `STAFF`, `CUSTOMER` role လိုအပ်ချက်ကို endpoint တစ်ခုစီအောက်တွင် ဖော်ပြထားသည်။ App ကို အခြား port ဖြင့် run ထားလျှင် `8080` ကိုပြောင်းပါ။

Error response ပုံစံ:

```json
{
  "success": false,
  "timestamp": "2026-09-10T15:00:00Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "fieldErrors": { "email": "must be a well-formed email address" }
}
```

---

## New Added Module — Customer Ticket PDF Download

### GET /customer/tickets/{ticketToken}/pdf

Access: CUSTOMER token required, own ACTIVE ticket only

request

```text
/customer/tickets/secure-ticket-token/pdf
Authorization: Bearer paste-customer-access-token-here
```

response

```text
Status: 200 OK
Content-Type: application/pdf
Content-Disposition: attachment; filename="quickseat-ticket-QS-ABC123.pdf"
Body: PDF binary file
```

Customer ၏ ကိုယ်ပိုင် ACTIVE ticket ကို booking details နှင့် QR image ပါဝင်သော printable PDF အဖြစ် download လုပ်ရန်ဖြစ်သည်။ PDF ကို request လာချိန်တွင် generate လုပ်ပြီး database သို့မဟုတ် Cloudinary တွင် မသိမ်းပါ။ အခြား customer ၏ ticket ဆိုလျှင် `404`၊ USED သို့မဟုတ် CANCELLED ticket ဆိုလျှင် `409` ပြန်ပေးသည်။

---

## New Added Module — Cinema Image Upload

### POST /admin/cinemas/image

Access: ADMIN token required

request

```text
Content-Type: multipart/form-data
form field name: file
form field value: select an image file (maximum 5 MB)
```

response

```json
{
  "success": true,
  "message": "Cinema image uploaded successfully",
  "data": {
    "url": "https://res.cloudinary.com/example/image/upload/cinema.jpg",
    "publicId": "quickseat/cinemas/example"
  }
}
```

Cinema image ကို dedicated Cloudinary cinema folder သို့ upload လုပ်ရန်ဖြစ်သည်။ Returned `url` ကို cinema create/update request ၏ `imageUrl` တွင် အသုံးပြုပါ။ Empty, non-image သို့မဟုတ် 5 MB ထက်ကြီးသော file ကို `400` ပြန်ပေးသည်။

---

## Public System Module

### GET /health

Access: Public

request

```text
/health
```

response

```json
{
  "success": true,
  "message": "QuickSeat API is running",
  "data": {
    "status": "UP",
    "timestamp": "2026-09-12T08:00:00Z"
  }
}
```

Use this endpoint to confirm that the backend is running and reachable.

---

## Public Discovery Module

### GET /cinemas

Access: Public

request

```text
/cinemas?search=QuickSeat&city=Yangon&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Cinemas retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "QuickSeat Yangon",
        "city": "Yangon",
        "imageUrl": "https://res.cloudinary.com/example/cinema.jpg",
        "active": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

Active cinema များကို search၊ city filter နှင့် pagination ဖြင့် ကြည့်ရန်ဖြစ်သည်။

### GET /cinemas/{cinemaId}

Access: Public

request

```text
/cinemas/1
```

response

```json
{
  "success": true,
  "message": "Cinema retrieved successfully",
  "data": {
    "id": 1,
    "name": "QuickSeat Yangon",
    "address": "No. 1, Example Road",
    "city": "Yangon",
    "phone": "09123456789",
    "imageUrl": "https://res.cloudinary.com/example/cinema.jpg",
    "active": true
  }
}
```

Active cinema တစ်ခု၏ အသေးစိတ်ကို ကြည့်ရန်ဖြစ်သည်။

### GET /showtimes

Access: Public

request

```text
/showtimes?movieId=1&cinemaId=1&date=2026-09-12
```

response

```json
{
  "success": true,
  "message": "Showtimes retrieved successfully",
  "data": [
    {
      "showtimeId": 1,
      "movieId": 1,
      "movieTitle": "Test Movie",
      "cinemaId": 1,
      "cinemaName": "QuickSeat Yangon",
      "screenId": 1,
      "screenName": "Screen 1",
      "startTime": "2026-09-12T10:00:00Z",
      "endTime": "2026-09-12T12:15:00Z",
      "normalPrice": 8000.00,
      "couplePrice": 15000.00
    }
  ]
}
```

အနာဂတ် ACTIVE showtime များကို movie၊ cinema နှင့် date ဖြင့် ရှာရန်ဖြစ်သည်။

---

## Authentication Module

### POST /auth/register

Access: Public

request

```json
{
  "name": "Aung Aung",
  "email": "aung.aung@example.com",
  "password": "Password123",
  "phone": "09123456789"
}
```

response

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "userId": 3,
    "name": "Aung Aung",
    "email": "aung.aung@example.com",
    "role": "CUSTOMER",
    "accessToken": "access-token",
    "refreshToken": "refresh-token",
    "tokenType": "Bearer"
  }
}
```

Customer account ဖန်တီးပြီး verification email ပို့ရန်ဖြစ်သည်။

### POST /auth/login

Access: Public

request

```json
{
  "email": "aung.aung@example.com",
  "password": "Password123"
}
```

response

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": 3,
    "name": "Aung Aung",
    "email": "aung.aung@example.com",
    "role": "CUSTOMER",
    "accessToken": "access-token",
    "refreshToken": "refresh-token",
    "tokenType": "Bearer"
  }
}
```

Email နှင့် password ဖြင့် login ဝင်ရန်ဖြစ်သည်။

### POST /auth/refresh

Access: Public, valid refresh token required

request

```json
{
  "refreshToken": "paste-refresh-token-here"
}
```

response

```json
{
  "success": true,
  "message": "Token refreshed",
  "data": {
    "accessToken": "new-access-token",
    "refreshToken": "new-refresh-token",
    "tokenType": "Bearer"
  }
}
```

Access token အသစ်ရယူရန်ဖြစ်သည်။

### POST /auth/logout

Access: Public, refresh token required

request

```json
{
  "refreshToken": "paste-current-refresh-token-here"
}
```

response

```json
{
  "success": true,
  "message": "Logout successful",
  "data": null
}
```

Refresh token ကို revoke လုပ်ရန်ဖြစ်သည်။

### GET /auth/verify-email

Access: Public

request

```text
/auth/verify-email?token=paste-verification-token-here
```

response

```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": null
}
```

Email ထဲက link ကိုနှိပ်ပြီး account verify လုပ်ရန်ဖြစ်သည်။

### POST /auth/resend-verification

Access: Public

request

```json
{
  "email": "aung.aung@example.com"
}
```

response

```json
{
  "success": true,
  "message": "If the account is eligible, a verification email has been sent",
  "data": null
}
```

Verification email ပြန်ပို့ရန်ဖြစ်သည်။

### POST /auth/forgot-password

Access: Public

request

```json
{
  "email": "aung.aung@example.com"
}
```

response

```json
{
  "success": true,
  "message": "If the account exists, a reset code has been sent",
  "data": null
}
```

Password reset OTP တောင်းရန်ဖြစ်သည်။

### POST /auth/verify-reset-otp

Access: Public

request

```json
{
  "email": "aung.aung@example.com",
  "otp": "123456"
}
```

response

```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": null
}
```

Password reset OTP ကို verify လုပ်ရန်ဖြစ်သည်။

### POST /auth/reset-password

Access: Public

request

```json
{
  "email": "aung.aung@example.com",
  "otp": "123456",
  "newPassword": "NewPassword123"
}
```

response

```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": null
}
```

Verified OTP ဖြင့် password အသစ်သတ်မှတ်ရန်ဖြစ်သည်။

---

### GET /oauth2/authorization/google

Access: Public (available only when `GOOGLE_OAUTH_ENABLED=true`)

request

```text
/oauth2/authorization/google
```

response

```text
302 redirect to Google sign-in
```

Starts the Google OAuth sign-in flow. It is a browser redirect endpoint, not a JSON/Bruno request. Google redirects back to the configured callback after consent.

---

## Public Movie Module

### GET /movies

Access: Public

request

```text
/movies?search=Test&status=NOW_SHOWING&language=English&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Movies retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Test Movie",
        "durationMinutes": 120,
        "posterUrl": "https://res.cloudinary.com/example/poster.jpg",
        "trailerUrl": "https://youtube.com/watch?v=example",
        "status": "NOW_SHOWING",
        "active": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

Active movie များကို search၊ status၊ language နှင့် pagination ဖြင့် ကြည့်ရန်ဖြစ်သည်။

### GET /movies/now-showing

Access: Public

request

```text
/movies/now-showing?search=Test&language=English&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Now-showing movies retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Test Movie",
        "status": "NOW_SHOWING",
        "active": true
      }
    ]
  }
}
```

Active `NOW_SHOWING` movie များကို ကြည့်ရန်ဖြစ်သည်။

### GET /movies/upcoming

Access: Public

request

```text
/movies/upcoming?search=Test&language=English&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Upcoming movies retrieved successfully",
  "data": {
    "content": [
      {
        "id": 2,
        "title": "Upcoming Movie",
        "status": "UPCOMING",
        "active": true
      }
    ]
  }
}
```

Active `UPCOMING` movie များကို ကြည့်ရန်ဖြစ်သည်။

### GET /movies/{movieId}

Access: Public

request

```text
/movies/1
```

response

```json
{
  "success": true,
  "message": "Movie retrieved successfully",
  "data": {
    "id": 1,
    "title": "Test Movie",
    "durationMinutes": 120,
    "status": "NOW_SHOWING",
    "active": true
  }
}
```

Active movie တစ်ခု၏ အသေးစိတ်ကို ကြည့်ရန်ဖြစ်သည်။

---

## Public Showtime Seat Map Module

### GET /showtimes/{showtimeId}/seats

Access: Public

request

```text
/showtimes/1/seats
```

response

```json
{
  "success": true,
  "message": "Showtime seats retrieved successfully",
  "data": {
    "showtimeId": 1,
    "movieTitle": "Test Movie",
    "screenName": "Screen 1",
    "startTime": "2026-09-12T10:00:00Z",
    "seats": [
      {
        "showtimeSeatId": 10,
        "seatId": 5,
        "rowName": "A",
        "seatNumber": 5,
        "seatType": "NORMAL",
        "price": 8000.00,
        "status": "AVAILABLE"
      }
    ]
  }
}
```

Showtime အလိုက် seat layout၊ price နှင့် availability ကိုကြည့်ရန်ဖြစ်သည်။

---

## Customer Seat Hold Module

### POST /customer/seat-holds

Access: verified CUSTOMER token required

request

```json
{
  "showtimeId": 1,
  "showtimeSeatIds": [
    10,
    11
  ]
}
```

response

```json
{
  "success": true,
  "message": "Seats held successfully",
  "data": {
    "bookingId": 20,
    "bookingReference": "QS-ABC123",
    "status": "PENDING",
    "totalAmount": 16000.00,
    "expiresAt": "2026-09-12T10:05:00Z",
    "remainingSeconds": 300,
    "selectedSeats": []
  }
}
```

Seats ကို ယာယီ hold လုပ်ပြီး PENDING booking ဖန်တီးရန်ဖြစ်သည်။

### GET /customer/seat-holds/{bookingReference}

Access: verified CUSTOMER token required, own booking only

request

```text
/customer/seat-holds/QS-ABC123
```

response

```json
{
  "success": true,
  "message": "Seat hold retrieved successfully",
  "data": {
    "bookingReference": "QS-ABC123",
    "status": "PENDING",
    "expiresAt": "2026-09-12T10:05:00Z",
    "remainingSeconds": 240
  }
}
```

ကိုယ်ပိုင် seat hold နှင့် countdown အခြေအနေကို ကြည့်ရန်ဖြစ်သည်။

### DELETE /customer/seat-holds/{bookingReference}

Access: verified CUSTOMER token required, own PENDING booking only

request

```text
/customer/seat-holds/QS-ABC123
```

response

```json
{
  "success": true,
  "message": "Seat hold released successfully",
  "data": {
    "bookingReference": "QS-ABC123",
    "status": "CANCELLED",
    "remainingSeconds": 0
  }
}
```

Payment မလုပ်မီ PENDING hold ကို cancel လုပ်ပြီး seats ပြန်လွှတ်ရန်ဖြစ်သည်။

---

## Customer Booking Module

### GET /customer/bookings

Access: verified CUSTOMER token required

request

```text
/customer/bookings?status=CONFIRMED&category=UPCOMING&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Bookings retrieved successfully",
  "data": {
    "content": [
      {
        "bookingReference": "QS-ABC123",
        "status": "CONFIRMED",
        "category": "UPCOMING",
        "totalAmount": 16000.00,
        "movieTitle": "Test Movie"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

ကိုယ်ပိုင် booking history ကို status၊ category၊ date နှင့် pagination ဖြင့် ကြည့်ရန်ဖြစ်သည်။

### GET /customer/bookings/{bookingReference}

Access: verified CUSTOMER token required, own booking only

request

```text
/customer/bookings/QS-ABC123
```

response

```json
{
  "success": true,
  "message": "Booking retrieved successfully",
  "data": {
    "bookingReference": "QS-ABC123",
    "status": "CONFIRMED",
    "category": "UPCOMING",
    "totalAmount": 16000.00,
    "movieTitle": "Test Movie",
    "cinemaName": "QuickSeat Yangon",
    "screenName": "Screen 1",
    "startTime": "2026-09-12T10:00:00Z",
    "seats": []
  }
}
```

ကိုယ်ပိုင် booking ၏ အသေးစိတ်ကို ကြည့်ရန်ဖြစ်သည်။

### PATCH /customer/bookings/{bookingReference}/cancel

Access: verified CUSTOMER token required, own eligible booking only

request

```text
/customer/bookings/QS-ABC123/cancel
```

response

```json
{
  "success": true,
  "message": "Booking cancelled successfully",
  "data": {
    "bookingReference": "QS-ABC123",
    "status": "CANCELLED",
    "cancelledAt": "2026-09-12T09:00:00Z"
  }
}
```

Eligible PENDING သို့မဟုတ် CONFIRMED booking ကို cancel လုပ်ရန်ဖြစ်သည်။

---

## Customer Mock Payment Module

### GET /customer/bookings/{bookingReference}/payment-summary

Access: verified CUSTOMER token required, own booking only

request

```text
/customer/bookings/QS-ABC123/payment-summary
```

response

```json
{
  "success": true,
  "message": "Payment summary retrieved successfully",
  "data": {
    "bookingReference": "QS-ABC123",
    "bookingStatus": "PENDING",
    "totalAmount": 16000.00,
    "expiresAt": "2026-09-12T10:05:00Z",
    "remainingSeconds": 240,
    "paymentStatus": null,
    "canPay": true
  }
}
```

Payment မလုပ်မီ booking amount နှင့် hold validity ကို စစ်ရန်ဖြစ်သည်။

### POST /customer/bookings/{bookingReference}/payments

Access: verified CUSTOMER token required, own PENDING booking only

request

```json
{
  "successful": true,
  "amount": 16000.00
}
```

response

```json
{
  "success": true,
  "message": "Payment processed successfully",
  "data": {
    "paymentReference": "PAY-ABC123",
    "paymentStatus": "SUCCESS",
    "amount": 16000.00,
    "bookingReference": "QS-ABC123",
    "bookingStatus": "CONFIRMED",
    "alreadyProcessed": false
  }
}
```

Mock payment success/failure ကို စမ်းသပ်ပြီး successful ဖြစ်လျှင် booking confirm လုပ်ရန်ဖြစ်သည်။

---

## Customer Ticket Module

### POST /customer/bookings/{bookingReference}/ticket

Access: verified CUSTOMER token required, own CONFIRMED and paid booking only

request

```text
/customer/bookings/QS-ABC123/ticket
```

response

```json
{
  "success": true,
  "message": "Ticket generated successfully",
  "data": {
    "ticketId": 40,
    "ticketToken": "secure-ticket-token",
    "status": "ACTIVE",
    "qrImageUrl": "/api/v1/customer/tickets/secure-ticket-token/qr",
    "bookingReference": "QS-ABC123",
    "alreadyGenerated": false
  }
}
```

Confirmed booking အတွက် single ticket နှင့် secure QR token ဖန်တီးရန်ဖြစ်သည်။

### GET /customer/bookings/{bookingReference}/ticket

Access: verified CUSTOMER token required, own booking only

request

```text
/customer/bookings/QS-ABC123/ticket
```

response

```json
{
  "success": true,
  "message": "Ticket retrieved successfully",
  "data": {
    "ticketToken": "secure-ticket-token",
    "status": "ACTIVE",
    "bookingReference": "QS-ABC123",
    "movieTitle": "Test Movie",
    "cinemaName": "QuickSeat Yangon",
    "screenName": "Screen 1",
    "totalAmount": 16000.00,
    "seats": []
  }
}
```

ကိုယ်ပိုင် ticket detail ကို ကြည့်ရန်ဖြစ်သည်။

### GET /customer/tickets/{ticketToken}/qr

Access: verified CUSTOMER token required, own ACTIVE ticket only

request

```text
/customer/tickets/secure-ticket-token/qr
/customer/tickets/secure-ticket-token/qr?download=true
```

response

```text
Status: 200 OK
Content-Type: image/png
Content-Disposition: inline; filename="quickseat-ticket.png"

When download=true:
Content-Disposition: attachment; filename="quickseat-ticket.png"
Body: PNG binary image
```

QR ကို browser တွင်ပြရန် သို့မဟုတ် PNG အဖြစ် download လုပ်ရန်ဖြစ်သည်။

PDF download endpoint ကို document အပေါ်ဆုံးရှိ `New Added Module — Customer Ticket PDF Download` တွင် ကြည့်ပါ။

---

## Cinema Module

### POST /admin/cinemas

Access: ADMIN token required

request

```json
{
  "name": "QuickSeat Yangon",
  "address": "No. 1, Example Road",
  "city": "Yangon",
  "phone": "09123456789",
  "imageUrl": "https://example.com/cinema.jpg"
}
```

response

```json
{
  "success": true,
  "message": "Cinema created successfully",
  "data": {
    "id": 1,
    "name": "QuickSeat Yangon",
    "address": "No. 1, Example Road",
    "city": "Yangon",
    "active": true
  }
}
```

Cinema အသစ်ဖန်တီးရန်ဖြစ်သည်။

### PUT /admin/cinemas/{cinemaId}

Access: ADMIN token required

request

```json
{
  "name": "QuickSeat Yangon Updated",
  "address": "No. 2, Example Road",
  "city": "Yangon",
  "phone": "09123456789",
  "imageUrl": "https://example.com/cinema.jpg"
}
```

response

```json
{
  "success": true,
  "message": "Cinema updated successfully",
  "data": {
    "id": 1,
    "name": "QuickSeat Yangon Updated",
    "active": true
  }
}
```

Cinema detail update လုပ်ရန်ဖြစ်သည်။

### GET /admin/cinemas

Access: ADMIN token required

request

```text
/admin/cinemas?search=Yangon&active=true&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Cinemas retrieved successfully",
  "data": {
    "content": [],
    "number": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

Cinema list ကို search, active နှင့် pagination ဖြင့်ကြည့်ရန်ဖြစ်သည်။

### GET /admin/cinemas/{cinemaId}

Access: ADMIN token required

request

```text
/admin/cinemas/1
```

response

```json
{
  "success": true,
  "message": "Cinema retrieved successfully",
  "data": {
    "id": 1,
    "name": "QuickSeat Yangon",
    "city": "Yangon",
    "active": true
  }
}
```

Cinema detail ကိုရယူရန်ဖြစ်သည်။

### PATCH /admin/cinemas/{cinemaId}/status

Access: ADMIN token required

request

```json
{
  "active": false
}
```

response

```json
{
  "success": true,
  "message": "Cinema status updated successfully",
  "data": {
    "id": 1,
    "active": false
  }
}
```

Cinema ကို activate/deactivate လုပ်ရန်ဖြစ်သည်။

## Screen Module

### POST /admin/cinemas/{cinemaId}/screens

Access: ADMIN token required

request

```json
{
  "name": "Screen 1"
}
```

response

```json
{
  "success": true,
  "message": "Screen created successfully",
  "data": {
    "id": 1,
    "cinemaId": 1,
    "name": "Screen 1",
    "active": true
  }
}
```

Cinema အောက်တွင် screen ဖန်တီးရန်ဖြစ်သည်။ Cinema တစ်ခုအတွင်း duplicate name သည် `409` ဖြစ်သည်။

### GET /admin/cinemas/{cinemaId}/screens

Access: ADMIN token required

request

```text
/admin/cinemas/1/screens
```

response

```json
{
  "success": true,
  "message": "Screens retrieved successfully",
  "data": [
    {
      "id": 1,
      "cinemaId": 1,
      "name": "Screen 1",
      "active": true
    }
  ]
}
```

Cinema ၏ screens ကိုရယူရန်ဖြစ်သည်။

### PUT /admin/screens/{screenId}

Access: ADMIN token required

request

```json
{
  "name": "Screen One"
}
```

response

```json
{
  "success": true,
  "message": "Screen updated successfully",
  "data": {
    "id": 1,
    "name": "Screen One",
    "active": true
  }
}
```

Screen name update လုပ်ရန်ဖြစ်သည်။

### PATCH /admin/screens/{screenId}/status

Access: ADMIN token required

request

```json
{
  "active": false
}
```

response

```json
{
  "success": true,
  "message": "Screen status updated successfully",
  "data": {
    "id": 1,
    "active": false
  }
}
```

Screen ကို activate/deactivate လုပ်ရန်ဖြစ်သည်။

## Seat Module

### POST /admin/screens/{screenId}/seats/layout

Access: ADMIN token required

request

```json
{
  "rows": [
    {
      "rowName": "A",
      "normalSeats": 8,
      "coupleSeats": 0
    },
    {
      "rowName": "B",
      "normalSeats": 6,
      "coupleSeats": 2
    }
  ]
}
```

response

```json
{
  "success": true,
  "message": "Seat layout generated successfully",
  "data": [
    {
      "id": 1,
      "screenId": 1,
      "rowName": "A",
      "seatNumber": 1,
      "seatType": "NORMAL",
      "active": true
    }
  ]
}
```

Row အလိုက် NORMAL/COUPLE physical seat layout generate လုပ်ရန်ဖြစ်သည်။

### GET /admin/screens/{screenId}/seats

Access: ADMIN token required

request

```text
/admin/screens/1/seats
```

response

```json
{
  "success": true,
  "message": "Seats retrieved successfully",
  "data": [
    {
      "id": 1,
      "screenId": 1,
      "rowName": "A",
      "seatNumber": 1,
      "seatType": "NORMAL",
      "active": true
    }
  ]
}
```

Screen ၏ physical seats ကိုကြည့်ရန်ဖြစ်သည်။

### PUT /admin/seats/{seatId}

Access: ADMIN token required

request

```json
{
  "rowName": "A",
  "seatNumber": 1,
  "seatType": "NORMAL"
}
```

response

```json
{
  "success": true,
  "message": "Seat updated successfully",
  "data": {
    "id": 1,
    "rowName": "A",
    "seatNumber": 1,
    "seatType": "NORMAL",
    "active": true
  }
}
```

Seat position/type update လုပ်ရန်ဖြစ်သည်။ Duplicate row + seat number သည် `409` ဖြစ်သည်။

### PATCH /admin/seats/{seatId}/status

Access: ADMIN token required

request

```json
{
  "active": false
}
```

response

```json
{
  "success": true,
  "message": "Seat status updated successfully",
  "data": {
    "id": 1,
    "active": false
  }
}
```

Seat ကို activate/deactivate လုပ်ရန်ဖြစ်သည်။

---

## Movie Management Module

Admin token လိုသည်။

### POST /admin/movies

Access: ADMIN token required

request

```json
{
  "title": "Example Movie",
  "description": "Short description",
  "durationMinutes": 120,
  "releaseDate": "2026-12-01",
  "language": "English",
  "genres": [
    "Action",
    "Drama"
  ],
  "ageRating": "PG-13",
  "director": "Example Director",
  "castText": "Actor A, Actor B",
  "posterUrl": "https://example.com/poster.jpg",
  "trailerUrl": "https://example.com/trailer",
  "status": "UPCOMING"
}
```

response

```json
{
  "success": true,
  "message": "Movie created successfully",
  "data": {
    "id": 1,
    "title": "Example Movie",
    "durationMinutes": 120,
    "status": "UPCOMING",
    "active": true
  }
}
```

Movie ဖန်တီးရန်ဖြစ်သည်။

### PUT /admin/movies/{movieId}

Access: ADMIN token required

request

```json
{
  "title": "Example Movie Updated",
  "description": "Updated description",
  "durationMinutes": 125,
  "releaseDate": "2026-12-01",
  "language": "English",
  "genres": [
    "Action"
  ],
  "ageRating": "PG-13",
  "director": "Example Director",
  "castText": "Actor A",
  "posterUrl": "https://example.com/poster.jpg",
  "trailerUrl": "https://example.com/trailer",
  "status": "NOW_SHOWING"
}
```

response

```json
{
  "success": true,
  "message": "Movie updated successfully",
  "data": {
    "id": 1,
    "title": "Example Movie Updated",
    "status": "NOW_SHOWING"
  }
}
```

Movie detail update လုပ်ရန်ဖြစ်သည်။ Required fields အားလုံးပို့ရမည်။

### GET /admin/movies

Access: ADMIN token required

request

```text
/admin/movies?search=Example&status=NOW_SHOWING&language=English&active=true&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Movies retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  }
}
```

Admin movie list ကို active/inactive အပါအဝင် filter လုပ်ရန်ဖြစ်သည်။

### GET /admin/movies/{movieId}

Access: ADMIN token required

request

```text
/admin/movies/1
```

response

```json
{
  "success": true,
  "message": "Movie retrieved successfully",
  "data": {
    "id": 1,
    "title": "Example Movie",
    "status": "NOW_SHOWING",
    "active": true
  }
}
```

Admin movie detail ကိုရယူရန်ဖြစ်သည်။

### PATCH /admin/movies/{movieId}/active

Access: ADMIN token required

request

```json
{
  "active": false
}
```

response

```json
{
  "success": true,
  "message": "Movie active status updated successfully",
  "data": {
    "id": 1,
    "active": false
  }
}
```

Movie operational availability ကိုပြောင်းရန်ဖြစ်သည်။

### PATCH /admin/movies/{movieId}/status

Access: ADMIN token required

request

```json
{
  "status": "NOW_SHOWING"
}
```

response

```json
{
  "success": true,
  "message": "Movie status updated successfully",
  "data": {
    "id": 1,
    "status": "NOW_SHOWING"
  }
}
```

Movie lifecycle status ကိုပြောင်းရန်ဖြစ်သည်။

### POST /admin/movies/poster

Access: ADMIN token required

request

```text
multipart/form-data
field name: file
value: choose image file
```

response

```json
{
  "success": true,
  "message": "Movie poster uploaded successfully",
  "data": {
    "url": "https://res.cloudinary.com/example/image/upload/example.jpg",
    "publicId": "quickseat/movies/example"
  }
}
```

Cloudinary သို့ poster upload လုပ်ရန်ဖြစ်သည်။ Cloudinary credentials မှန်ရမည်။

---

## Showtime Module

အောက်ပါ management endpoints များအတွက် Admin token လိုသည်။

### POST /admin/showtimes

Access: ADMIN token required

request

```json
{
  "movieId": 1,
  "screenId": 1,
  "startTime": "2026-12-01T06:30:00Z",
  "normalPrice": 5000,
  "couplePrice": 9000,
  "cleaningBufferMinutes": 15
}
```

response

```json
{
  "success": true,
  "message": "Showtime created successfully",
  "data": {
    "id": 1,
    "movieId": 1,
    "movieTitle": "Example Movie",
    "screenId": 1,
    "screenName": "Screen 1",
    "cinemaId": 1,
    "startTime": "2026-12-01T06:30:00Z",
    "endTime": "2026-12-01T08:45:00Z",
    "normalPrice": 5000,
    "couplePrice": 9000,
    "status": "ACTIVE"
  }
}
```

End time ကို movie duration + cleaning buffer ဖြင့် backend ကတွက်သည်။ Inactive records သို့မဟုတ် same screen active overlap ကို reject လုပ်သည်။

### PUT /admin/showtimes/{showtimeId}

Access: ADMIN token required

request

```json
{
  "movieId": 1,
  "screenId": 1,
  "startTime": "2026-12-01T09:00:00Z",
  "normalPrice": 5500,
  "couplePrice": 10000,
  "cleaningBufferMinutes": 15
}
```

response

```json
{
  "success": true,
  "message": "Showtime updated successfully",
  "data": {
    "id": 1,
    "startTime": "2026-12-01T09:00:00Z",
    "status": "ACTIVE"
  }
}
```

Showtime update လုပ်ရန်ဖြစ်သည်။ Overlap rule ကိုထပ်စစ်သည်။

### PATCH /admin/showtimes/{showtimeId}/cancel

Access: ADMIN token required

request

```text
No request body
```

response

```json
{
  "success": true,
  "message": "Showtime cancelled successfully",
  "data": {
    "id": 1,
    "status": "CANCELLED"
  }
}
```

Showtime ကို cancel လုပ်ရန်ဖြစ်သည်။ Cancelled showtime သည် schedule overlap မပိတ်တော့ပါ။

### GET /admin/showtimes

Access: ADMIN token required

request

```text
/admin/showtimes?movieId=1&cinemaId=1&date=2026-12-01&status=ACTIVE&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Showtimes retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  }
}
```

Movie, cinema, date, status ဖြင့် showtime list ကိုကြည့်ရန်ဖြစ်သည်။

### GET /admin/showtimes/{showtimeId}

Access: ADMIN token required

request

```text
/admin/showtimes/1
```

response

```json
{
  "success": true,
  "message": "Showtime retrieved successfully",
  "data": {
    "id": 1,
    "movieTitle": "Example Movie",
    "screenName": "Screen 1",
    "status": "ACTIVE"
  }
}
```

Showtime detail ကိုရယူရန်ဖြစ်သည်။

### POST /admin/showtimes/{showtimeId}/seats/generate

Access: ADMIN token required

request

```text
No request body
```

response

```json
{
  "success": true,
  "message": "Showtime seat inventory generated successfully",
  "data": []
}
```

Assigned screen layout မှ showtime inventory ဖန်တီးသည်။ ထပ် generate လုပ်လျှင် `409` ဖြစ်သည်။

---

## Staff Operations Module

Staff token လိုသည်။ Assigned cinema data ကိုသာ access လုပ်နိုင်သည်။

### GET /staff/cinema

Access: STAFF token required

request

```text
No request body
```

response

```json
{
  "success": true,
  "message": "Assigned cinema retrieved successfully",
  "data": {
    "id": 1,
    "name": "QuickSeat Yangon",
    "city": "Yangon",
    "active": true
  }
}
```

Logged-in staff ၏ cinema ကိုရယူရန်ဖြစ်သည်။

### GET /staff/showtimes

Access: STAFF token required

request

```text
/staff/showtimes?movieId=1&date=2026-12-01&status=ACTIVE&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Cinema showtimes retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  }
}
```

Assigned cinema showtimes ကို filter/pagination ဖြင့်ကြည့်ရန်ဖြစ်သည်။

### GET /staff/showtimes/{showtimeId}

Access: STAFF token required

request

```text
/staff/showtimes/1
```

response

```json
{
  "success": true,
  "message": "Showtime retrieved successfully",
  "data": {
    "id": 1,
    "cinemaId": 1,
    "movieTitle": "Example Movie",
    "status": "ACTIVE"
  }
}
```

Assigned cinema showtime detail ကိုရယူရန်ဖြစ်သည်။

### GET /staff/showtimes/{showtimeId}/seats

Access: STAFF token required

request

```text
/staff/showtimes/1/seats
```

response

```json
{
  "success": true,
  "message": "Showtime seat status retrieved successfully",
  "data": {
    "showtimeId": 1,
    "seats": []
  }
}
```

Assigned cinema showtime seat status ကိုကြည့်ရန်ဖြစ်သည်။

### GET /staff/bookings

Access: STAFF token required

request

```text
/staff/bookings?status=CONFIRMED&showtimeId=1&date=2026-12-01&search=QS-ABC123&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Cinema bookings retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  }
}
```

Assigned cinema bookings ကို filter/search/pagination ဖြင့်ကြည့်ရန်ဖြစ်သည်။

### GET /staff/bookings/{bookingReference}

Access: STAFF token required

request

```text
/staff/bookings/QS-ABC123
```

response

```json
{
  "success": true,
  "message": "Booking retrieved successfully",
  "data": {
    "bookingReference": "QS-ABC123",
    "status": "CONFIRMED",
    "cinemaId": 1
  }
}
```

Assigned cinema booking detail ကိုရယူရန်ဖြစ်သည်။

### GET /staff/tickets/{ticketToken}

Access: STAFF token required

request

```text
/staff/tickets/secure-ticket-token
```

response

```json
{
  "success": true,
  "message": "Ticket retrieved successfully",
  "data": {
    "ticketToken": "secure-ticket-token",
    "ticketStatus": "ACTIVE",
    "bookingStatus": "CONFIRMED"
  }
}
```

Ticket ကို validate မလုပ်ခင်စစ်ရန်ဖြစ်သည်။

### POST /staff/tickets/validate

Access: STAFF token required

request

```json
{
  "ticketToken": "secure-ticket-token"
}
```

response

```json
{
  "success": true,
  "message": "Ticket validated successfully",
  "data": {
    "ticketToken": "secure-ticket-token",
    "result": "VALID",
    "ticketStatus": "USED",
    "bookingStatus": "USED"
  }
}
```

Valid assigned-cinema ticket ကို single-use validate လုပ်သည်။ Repeated scan/cancelled/wrong cinema ticket ကို reject လုပ်သည်။

---

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

## Admin Customer Management Module

Admin token လိုသည်။

### GET /admin/customers

Access: ADMIN token required

request

```text
/admin/customers?search=aung&active=true&emailVerified=true&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Customers retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  }
}
```

Customer list ကို search, active, verification status ဖြင့်ကြည့်ရန်ဖြစ်သည်။

### GET /admin/customers/{userId}

Access: ADMIN token required

request

```text
/admin/customers/3
```

response

```json
{
  "success": true,
  "message": "Customer retrieved successfully",
  "data": {
    "id": 3,
    "name": "Aung Aung",
    "email": "aung.aung@example.com",
    "active": true,
    "emailVerified": true
  }
}
```

Customer detail ကိုရယူရန်ဖြစ်သည်။

### PATCH /admin/customers/{userId}/active

Access: ADMIN token required

request

```json
{
  "active": false
}
```

response

```json
{
  "success": true,
  "message": "Customer active status updated successfully",
  "data": {
    "id": 3,
    "active": false
  }
}
```

Customer account activate/deactivate လုပ်ရန်ဖြစ်သည်။

## Admin Staff Management Module

### POST /admin/staff

Access: ADMIN token required

request

```json
{
  "name": "Staff One",
  "email": "staff1@example.com",
  "password": "Password123",
  "phone": "09987654321",
  "cinemaId": 1
}
```

response

```json
{
  "success": true,
  "message": "Staff account created successfully",
  "data": {
    "id": 4,
    "name": "Staff One",
    "email": "staff1@example.com",
    "role": "STAFF",
    "cinemaId": 1,
    "active": true
  }
}
```

Cinema assignment ပါသော staff account ဖန်တီးရန်ဖြစ်သည်။ Password ကို hash ဖြင့်သိမ်းသည်။

### GET /admin/staff

Access: ADMIN token required

request

```text
/admin/staff?search=staff&active=true&cinemaId=1&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Staff accounts retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  }
}
```

Staff list ကို search, active, cinema ဖြင့်ကြည့်ရန်ဖြစ်သည်။

### GET /admin/staff/{staffId}

Access: ADMIN token required

request

```text
/admin/staff/4
```

response

```json
{
  "success": true,
  "message": "Staff account retrieved successfully",
  "data": {
    "id": 4,
    "name": "Staff One",
    "email": "staff1@example.com",
    "cinemaId": 1,
    "active": true
  }
}
```

Staff detail ကိုရယူရန်ဖြစ်သည်။

### PUT /admin/staff/{staffId}

Access: ADMIN token required

request

```json
{
  "name": "Staff One Updated",
  "email": "staff1@example.com",
  "phone": "09987654321"
}
```

response

```json
{
  "success": true,
  "message": "Staff account updated successfully",
  "data": {
    "id": 4,
    "name": "Staff One Updated",
    "email": "staff1@example.com"
  }
}
```

Staff profile update လုပ်ရန်ဖြစ်သည်။ Password ပြောင်းလိုလျှင် `password` field ကိုထည့်ပါ။

### PATCH /admin/staff/{staffId}/active

Access: ADMIN token required

request

```json
{
  "active": false
}
```

response

```json
{
  "success": true,
  "message": "Staff active status updated successfully",
  "data": {
    "id": 4,
    "active": false
  }
}
```

Staff account activate/deactivate လုပ်ရန်ဖြစ်သည်။

### PATCH /admin/staff/{staffId}/cinema

Access: ADMIN token required

request

```json
{
  "cinemaId": 2
}
```

response

```json
{
  "success": true,
  "message": "Staff cinema assignment updated successfully",
  "data": {
    "id": 4,
    "cinemaId": 2
  }
}
```

Staff cinema assignment ကိုပြောင်းရန်ဖြစ်သည်။

---

## Admin Booking Management Module

Admin token လိုသည်။

### GET /admin/bookings

Access: ADMIN token required

request

```text
/admin/bookings?status=CONFIRMED&cinemaId=1&movieId=1&showtimeId=1&date=2026-12-01&search=aung.aung@example.com&page=0&size=20
```

response

```json
{
  "success": true,
  "message": "Bookings retrieved successfully",
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "last": true
  }
}
```

System-wide booking list ကို status/cinema/movie/showtime/date/search ဖြင့်ကြည့်ရန်ဖြစ်သည်။

### GET /admin/bookings/{bookingReference}

Access: ADMIN token required

request

```text
/admin/bookings/QS-ABC123
```

response

```json
{
  "success": true,
  "message": "Booking retrieved successfully",
  "data": {
    "bookingReference": "QS-ABC123",
    "status": "CONFIRMED",
    "totalAmount": 10000,
    "seats": []
  }
}
```

Booking detail ကိုရယူရန်ဖြစ်သည်။

### PATCH /admin/bookings/{bookingReference}/cancel

Access: ADMIN token required

request

```text
No request body
```

response

```json
{
  "success": true,
  "message": "Booking cancelled successfully",
  "data": {
    "bookingReference": "QS-ABC123",
    "status": "CANCELLED"
  }
}
```

Eligible booking ကို admin က cancel လုပ်ရန်ဖြစ်သည်။ PENDING seats ကို release လုပ်ပြီး eligible CONFIRMED ticket ကို cancel လုပ်သည်။

---

## Dashboard Module

Admin token လိုသည်။ Common filters: `from=YYYY-MM-DD`, `to=YYYY-MM-DD`, `cinemaId`, `movieId`.

### GET /admin/dashboard/summary

Access: ADMIN token required

request

```text
/admin/dashboard/summary?from=2026-09-01&to=2026-09-30&cinemaId=1&movieId=1
```

response

```json
{
  "success": true,
  "message": "Dashboard summary retrieved successfully",
  "data": {
    "totalBookings": 25,
    "confirmedBookings": 15,
    "todayBookings": 3,
    "totalRevenue": 150000,
    "occupancyRate": 42.5,
    "cancellationRate": 8
  }
}
```

Booking/revenue/occupancy/cancellation summary ကိုရယူရန်ဖြစ်သည်။ Revenue သည် SUCCESS payment + CONFIRMED/USED booking များသာပါသည်။

### GET /admin/dashboard/movies

Access: ADMIN token required

request

```text
/admin/dashboard/movies?from=2026-09-01&to=2026-09-30&cinemaId=1&movieId=1&limit=50
```

response

```json
{
  "success": true,
  "message": "Movie performance retrieved successfully",
  "data": []
}
```

Movie performance list ကိုရယူရန်ဖြစ်သည်။ `limit` 1–100 ဖြစ်ရမည်။

### GET /admin/dashboard/movies/top

Access: ADMIN token required

request

```text
/admin/dashboard/movies/top?from=2026-09-01&to=2026-09-30&limit=10
```

response

```json
{
  "success": true,
  "message": "Most-booked movies retrieved successfully",
  "data": []
}
```

Most-booked movies ကိုရယူရန်ဖြစ်သည်။ `limit` 1–50 ဖြစ်ရမည်။

### GET /admin/dashboard/cinemas

Access: ADMIN token required

request

```text
/admin/dashboard/cinemas?from=2026-09-01&to=2026-09-30&cinemaId=1
```

response

```json
{
  "success": true,
  "message": "Cinema performance retrieved successfully",
  "data": []
}
```

Cinema-wise bookings, revenue, occupancy ကိုရယူရန်ဖြစ်သည်။

### GET /admin/dashboard/revenue/daily

Access: ADMIN token required

request

```text
/admin/dashboard/revenue/daily?from=2026-09-01&to=2026-09-30&cinemaId=1
```

response

```json
{
  "success": true,
  "message": "Daily revenue trend retrieved successfully",
  "data": []
}
```

Daily revenue trend ကိုရယူရန်ဖြစ်သည်။

### GET /admin/dashboard/revenue/monthly

Access: ADMIN token required

request

```text
/admin/dashboard/revenue/monthly?from=2026-01-01&to=2026-12-31&movieId=1
```

response

```json
{
  "success": true,
  "message": "Monthly revenue trend retrieved successfully",
  "data": []
}
```

Monthly revenue trend ကိုရယူရန်ဖြစ်သည်။

---

## Bruno Test Order

```text
1. GET /health
2. POST /auth/register
3. Email verification link ကို click
4. POST /auth/login and save CUSTOMER token
5. ADMIN: cinema -> screen -> seat layout -> movie -> showtime
6. ADMIN: POST /admin/showtimes/{showtimeId}/seats/generate
7. CUSTOMER: GET seat map -> POST seat hold -> POST payment -> POST ticket
8. ADMIN: staff create/assign cinema
9. STAFF: POST /staff/tickets/validate
10. ADMIN: dashboard APIs
```

Actual JWT, refresh token, OTP, ticket token, password နှင့် database credential များကို documentation/Bruno collection ထဲ hard-code မလုပ်ပါနှင့်.
