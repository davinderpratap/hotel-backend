package com.hotel.reservation.service;

import com.hotel.reservation.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Service for managing hotel room bookings.
 */
@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    // Constants for hotel configuration
    public static final int MAX_FLOORS = 10;
    public static final int ROOMS_PER_FLOOR = 10;
    public static final int TOP_FLOOR = 10;
    public static final int TOP_FLOOR_ROOMS = 7;
    public static final int MAX_BOOKABLE_ROOMS = 5;

    // Response keys and messages
    public static final String STATUS = "status";
    public static final String SUCCESS = "success";
    public static final String ERROR = "error";
    public static final String MESSAGE = "message";
    public static final String ROOM_LIST = "roomlist";
    public static final String BOOK_SUCCESS_MSG = "Rooms booked successfully";
    public static final String BOOK_LIMIT_MSG = "You can only book a maximum of 5 rooms at a time.";
    public static final String NOT_ENOUGH_ROOMS_MSG = "Not enough rooms available!";
    public static final String NO_ROOMS_MSG = "No rooms available";

    // Map to hold rooms for each floor
    private final Map<Integer, List<Room>> hotelFloors = new HashMap<>();

    /**
     * Initializes the hotel room structure after bean construction.
     */
    @PostConstruct
    public void init() {
        for (int floor = 1; floor < TOP_FLOOR; floor++) {
            List<Room> rooms = new ArrayList<>();
            int baseRoomNumber = floor * 100;
            for (int i = 1; i <= ROOMS_PER_FLOOR; i++) {
                rooms.add(new Room(floor, baseRoomNumber + i));
            }
            hotelFloors.put(floor, rooms);
        }

        // Top floor has a different number of rooms
        List<Room> topFloorRooms = new ArrayList<>();
        for (int i = 1; i <= TOP_FLOOR_ROOMS; i++) {
            topFloorRooms.add(new Room(TOP_FLOOR, 1000 + i));
        }
        hotelFloors.put(TOP_FLOOR, topFloorRooms);

        logger.info("Hotel floors and rooms initialized");
    }

    /**
     * Returns all rooms in the hotel.
     */
    public Map<Integer, List<Room>> getAllRooms() {
        return hotelFloors;
    }

    /**
     * Calculates travel time between two rooms.
     */
    private int travelTime(Room r1, Room r2) {
        int verticalTime = 2 * Math.abs(r1.getFloor() - r2.getFloor());
        int horizontalTime = Math.abs(r1.getRoomNumber() - r2.getRoomNumber());
        return verticalTime + horizontalTime;
    }

    /**
     * Calculates total travel time for a list of rooms.
     */
    private int totalTravelTime(List<Room> rooms) {
        if (rooms.isEmpty() || rooms.size() == 1) return 0;
        rooms.sort(Comparator.comparingInt(Room::getFloor)
                .thenComparingInt(Room::getRoomNumber));
        int totalTime = 0;
        for (int i = 0; i < rooms.size() - 1; i++) {
            totalTime += travelTime(rooms.get(i), rooms.get(i + 1));
        }
        return totalTime;
    }

    /**
     * Generates all combinations of n rooms from the given list.
     */
    private List<List<Room>> combinations(List<Room> rooms, int n) {
        List<List<Room>> result = new ArrayList<>();
        combineHelper(rooms, n, 0, new ArrayList<>(), result);
        return result;
    }

    private void combineHelper(List<Room> rooms, int n, int start, List<Room> current, List<List<Room>> result) {
        if (current.size() == n) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < rooms.size(); i++) {
            current.add(rooms.get(i));
            combineHelper(rooms, n, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    /**
     * Attempts to book the requested number of rooms.
     * Synchronized to handle concurrent requests safely.
     */
    public synchronized Map<String, Object> bookRooms(int numberOfRooms) {
        logger.info("Attempting to book {} rooms", numberOfRooms);

        Map<String, Object> response = new HashMap<>();
        if (numberOfRooms > MAX_BOOKABLE_ROOMS) {
            response.put(STATUS, ERROR);
            response.put(MESSAGE, BOOK_LIMIT_MSG);
            response.put(ROOM_LIST, Collections.emptyList());
            logger.warn("Booking failed: {}", BOOK_LIMIT_MSG);
            return response;
        }

        // Try to book on a single floor first
        for (int floor = 1; floor <= MAX_FLOORS; floor++) {
            List<Room> availableRooms = new ArrayList<>();
            List<Room> floorRooms = hotelFloors.get(floor);
            if (floorRooms == null) continue;

            for (Room room : floorRooms) {
                if (!room.isBooked()) {
                    availableRooms.add(room);
                    if (availableRooms.size() == numberOfRooms) break;
                }
            }

            if (availableRooms.size() == numberOfRooms) {
                for (Room r : availableRooms) r.setBooked(true);
                response.put(STATUS, SUCCESS);
                response.put(MESSAGE, BOOK_SUCCESS_MSG);
                response.put(ROOM_LIST, availableRooms);
                logger.info("Successfully booked rooms on floor {}: {}", floor, availableRooms);
                return response;
            }
        }

        // If not possible on a single floor, try across floors
        List<Room> allAvailableRooms = new ArrayList<>();
        for (List<Room> rooms : hotelFloors.values()) {
            for (Room r : rooms) {
                if (!r.isBooked()) {
                    allAvailableRooms.add(r);
                }
            }
        }

        if (allAvailableRooms.size() < numberOfRooms) {
            response.put(STATUS, ERROR);
            response.put(MESSAGE, NOT_ENOUGH_ROOMS_MSG);
            response.put(ROOM_LIST, Collections.emptyList());
            logger.warn("Booking failed: {}", NOT_ENOUGH_ROOMS_MSG);
            return response;
        }

        // Find the combination with minimum travel time
        List<List<Room>> combos = combinations(allAvailableRooms, numberOfRooms);
        List<Room> bestCombo = null;
        int minTravelTime = Integer.MAX_VALUE;

        for (List<Room> combo : combos) {
            int tTime = totalTravelTime(combo);
            if (tTime < minTravelTime) {
                minTravelTime = tTime;
                bestCombo = combo;
            }
        }

        if (bestCombo != null) {
            for (Room r : bestCombo) r.setBooked(true);
            response.put(STATUS, SUCCESS);
            response.put(MESSAGE, BOOK_SUCCESS_MSG);
            response.put(ROOM_LIST, bestCombo);
            logger.info("Successfully booked rooms across floors: {}", bestCombo);
            return response;
        }

        response.put(STATUS, ERROR);
        response.put(MESSAGE, NO_ROOMS_MSG);
        response.put(ROOM_LIST, Collections.emptyList());
        logger.warn("Booking failed: {}", NO_ROOMS_MSG);
        return response;
    }

    /**
     * Resets all room bookings.
     * Synchronized for thread safety.
     */
    public synchronized void resetBookings() {
        logger.info("Resetting all bookings");
        for (List<Room> rooms : hotelFloors.values()) {
            for (Room room : rooms) {
                room.setBooked(false);
            }
        }
    }

    /**
     * Returns a list of all currently booked rooms.
     */
    public List<Room> getBookedRooms() {
        List<Room> bookedRooms = new ArrayList<>();
        for (List<Room> rooms : hotelFloors.values()) {
            for (Room room : rooms) {
                if (room.isBooked()) {
                    bookedRooms.add(room);
                }
            }
        }
        return bookedRooms;
    }
}
