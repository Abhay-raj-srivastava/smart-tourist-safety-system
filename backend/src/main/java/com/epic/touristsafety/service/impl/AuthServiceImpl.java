package com.epic.touristsafety.service.impl;

import com.epic.touristsafety.dto.request.LoginRequest;
import com.epic.touristsafety.dto.request.RegisterRequest;
import com.epic.touristsafety.dto.response.AuthResponse;
import com.epic.touristsafety.entity.User;
import com.epic.touristsafety.exception.BusinessValidationException;
import com.epic.touristsafety.repository.UserRepository;
import com.epic.touristsafety.security.JwtService;
import com.epic.touristsafety.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) {
        // 1. Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessValidationException("Email is already registered.");
        }

        // 2. Create the new User entity
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .isActive(true)
                .build();

        // 3. Save to database
        userRepository.save(user);

        // 4. Generate JWT Token
        String jwtToken = jwtService.generateToken(user);

        // 5. Return Response
        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. Authenticate user credentials (this throws exception if bad credentials)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Fetch User from DB
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessValidationException("User not found."));

        // 3. Generate JWT Token
        String jwtToken = jwtService.generateToken(user);

        // 4. Return Response
        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}