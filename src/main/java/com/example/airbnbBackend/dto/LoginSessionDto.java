package com.example.airbnbBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Internal result used to put the refresh token in an HttpOnly cookie while
 * returning only the access token and non-sensitive profile to the client.
 */
@Getter
@AllArgsConstructor
public class LoginSessionDto {
    private final String accessToken;
    private final String refreshToken;
    private final UserDto user;
}
