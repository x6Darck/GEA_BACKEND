package com.calendario.callapp.callapp_backend.service;

import com.calendario.callapp.callapp_backend.dto.request.AuthRequest;
import com.calendario.callapp.callapp_backend.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse login(AuthRequest request);
}