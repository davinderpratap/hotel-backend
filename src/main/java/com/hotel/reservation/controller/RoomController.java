package com.hotel.reservation.controller;

import com.hotel.reservation.model.Room;
import com.hotel.reservation.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final BookingService bookingService;

    @Autowired
    public RoomController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Retrieves all rooms grouped by their floor.
     * @return Map of floor to list of rooms.
     */
    @GetMapping("/all")
    public Map<Integer, List<Room>> getAllRooms() {
        return bookingService.getAllRooms();
    }

    /**
     * Books the requested number of rooms if available.
     * @param numberOfRooms Number of rooms to book.
     * @return Booking details or error message.
     */
    @PostMapping("/book")
    public ResponseEntity<?> bookRooms(@RequestParam int numberOfRooms) {
        if (numberOfRooms <= 0) {
            return ResponseEntity.badRequest().body("Number of rooms must be positive.");
        }

        Map<String, Object> bookingResponseMap = bookingService.bookRooms(numberOfRooms);

        if (BookingService.ERROR.equals(bookingResponseMap.get(BookingService.STATUS))) {
            // Return HTTP 409 Conflict with error message
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(bookingResponseMap.get(BookingService.MESSAGE));
        }

        return ResponseEntity.ok(bookingResponseMap);
    }

    /**
     * Resets all room bookings.
     * @return Confirmation message.
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetBookings() {
        bookingService.resetBookings();
        return ResponseEntity.ok("All bookings have been reset.");
    }

    /**
     * Retrieves the list of currently booked rooms.
     * @return List of booked rooms.
     */
    @GetMapping("/bookedRooms")
    public List<Room> getBookedRooms() {
        return bookingService.getBookedRooms();
    }
}
