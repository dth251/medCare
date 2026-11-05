package com.medcare.clinic.service;

import com.medcare.clinic.dto.auth.JwtResponse;
import com.medcare.clinic.dto.auth.LoginRequest;
import com.medcare.clinic.dto.auth.MeResponse;
import com.medcare.clinic.dto.auth.UpdateProfileRequest;

import java.util.List;

public interface AuthService {
    JwtResponse login(LoginRequest request);
    MeResponse me(String username, List<String> roles);
    MeResponse updateProfile(String username, List<String> roles, UpdateProfileRequest request);
}
