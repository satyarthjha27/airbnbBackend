package com.example.airbnbBackend.security;

import com.example.airbnbBackend.dto.LoginDto;
import com.example.airbnbBackend.dto.LoginSessionDto;
import com.example.airbnbBackend.dto.SignUpRequestDto;
import com.example.airbnbBackend.dto.UserDto;
import com.example.airbnbBackend.entity.User;
import com.example.airbnbBackend.entity.enums.Role;
import com.example.airbnbBackend.exception.ResourceNotFoundException;
import com.example.airbnbBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;

    public UserDto signUp(SignUpRequestDto signUpRequestDto) {

        User user = userRepository.findByEmail(signUpRequestDto.getEmail()).orElse(null);

        if (user != null) {
            throw new RuntimeException("User is already present with same email id");
        }

        User newUser = new User();
        newUser.setEmail(signUpRequestDto.getEmail().trim().toLowerCase());
        newUser.setName(signUpRequestDto.getName().trim());
        Role requestedRole = signUpRequestDto.getRole() == Role.HOTEL_MANAGER
                ? Role.HOTEL_MANAGER
                : Role.GUEST;
        newUser.setRole(Set.of(requestedRole));
        newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        newUser = userRepository.save(newUser);

        return toUserDto(newUser);
    }

    public LoginSessionDto login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getEmail(), loginDto.getPassword()
        ));

        User user = (User) authentication.getPrincipal();

        return new LoginSessionDto(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                toUserDto(user)
        );
    }

    public String refreshToken(String refreshToken) {
        Long id = jwtService.getUserIdFromToken(refreshToken);

        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: "+id));
        return jwtService.generateAccessToken(user);
    }

    private UserDto toUserDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setName(user.getName());
        userDto.setRoles(Set.copyOf(user.getRole()));
        return userDto;
    }

}
