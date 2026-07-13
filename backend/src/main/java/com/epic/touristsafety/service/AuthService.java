package com.epic.touristsafety.service;

import com.epic.touristsafety.dto.request.LoginRequest;
import com.epic.touristsafety.dto.request.RegisterRequest;
import com.epic.touristsafety.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}