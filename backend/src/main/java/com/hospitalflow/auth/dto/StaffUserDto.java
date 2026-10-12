package com.hospitalflow.auth.dto;

import com.hospitalflow.auth.User;
import java.time.LocalDateTime;

public record StaffUserDto(
        Long id,
        String name,
        String email,
        User.Role role,
        LocalDateTime createdAt
) {}
