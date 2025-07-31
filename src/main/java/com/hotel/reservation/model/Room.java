package com.hotel.reservation.model;

public class Room{

    private int roomNumber;
    private int floor;
    private boolean isBooked;

    public int getRoomNumber() {
        return roomNumber;
    }

    public int getFloor() {
        return floor;
    }

    public boolean isBooked() {
        return isBooked;
    }

    public void setBooked(boolean isBooked) {
        this.isBooked = isBooked;
    }

    public Room(int floor,int roomNumber){
        this.floor = floor;
        this.roomNumber = roomNumber;
        this.isBooked = false;
    }
    
}