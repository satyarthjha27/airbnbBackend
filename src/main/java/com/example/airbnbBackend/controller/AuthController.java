package com.example.airbnbBackend.controller;


import com.example.airbnbBackend.dto.LoginDto;
import com.example.airbnbBackend.dto.LoginResponseDto;
import com.example.airbnbBackend.dto.LoginSessionDto;
import com.example.airbnbBackend.dto.SignUpRequestDto;
import com.example.airbnbBackend.dto.UserDto;
import com.example.airbnbBackend.security.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@RequestBody SignUpRequestDto signUpRequestDto) {
        return new ResponseEntity<>(authService.signUp(signUpRequestDto), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginDto loginDto, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        LoginSessionDto session = authService.login(loginDto);

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", session.getRefreshToken())
                .httpOnly(true)
                .secure(httpServletRequest.isSecure())
                .sameSite(httpServletRequest.isSecure() ? "None" : "Lax")
                .path("/api/v1/auth")
                .maxAge(30L * 24 * 60 * 60)
                .build();
        httpServletResponse.addHeader("Set-Cookie", refreshCookie.toString());
        return ResponseEntity.ok(new LoginResponseDto(session.getAccessToken(), session.getUser()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(HttpServletRequest request) {
        String refreshToken = Arrays.stream(request.getCookies() == null ? new jakarta.servlet.http.Cookie[0] : request.getCookies()).
                filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new AuthenticationServiceException("Refresh token not found inside the Cookies"));

        String accessToken = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(new LoginResponseDto(accessToken, null));
    }

}
