package com.portfolio.hotel.shared.error;

public class ReservationConflictException extends RuntimeException {
    public ReservationConflictException(String message) {
        super(message);
    }
}

