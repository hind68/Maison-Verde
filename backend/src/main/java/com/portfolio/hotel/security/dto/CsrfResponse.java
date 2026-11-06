package com.portfolio.hotel.security.dto;

public record CsrfResponse(String token, String headerName) {
}
