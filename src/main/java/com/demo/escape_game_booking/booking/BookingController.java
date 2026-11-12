package com.demo.escape_game_booking.booking;

import com.demo.escape_game_booking.auth.CurrentUser;
import com.demo.escape_game_booking.booking.dto.BookingResponse;
import com.demo.escape_game_booking.booking.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        BookingResponse created = bookingService.create(request, CurrentUser.from(jwt));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/me")
    public List<BookingResponse> getMyBookings(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.findMine(CurrentUser.from(jwt));
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return bookingService.findById(id, CurrentUser.from(jwt));
    }

    @PatchMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return bookingService.cancel(id, CurrentUser.from(jwt));
    }
}