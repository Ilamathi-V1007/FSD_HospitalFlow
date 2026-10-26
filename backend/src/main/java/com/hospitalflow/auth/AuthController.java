package com.hospitalflow.auth;

import com.hospitalflow.auth.dto.AuthResponse;
import com.hospitalflow.auth.dto.LoginRequest;
import com.hospitalflow.auth.dto.RegisterRequest;
import com.hospitalflow.auth.dto.StaffUserDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Public registration (left available for fallback / initial seeding)
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    // Admin-only Staff Creation endpoint
    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffUserDto> createStaffAccount(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.createStaffUser(request));
    }

    // Admin-only Staff Directory endpoint
    @GetMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffUserDto>> getAllStaff() {
        return ResponseEntity.ok(authService.getAllStaff());
    }

    @DeleteMapping("/staff/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        authService.deleteStaff(id);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
