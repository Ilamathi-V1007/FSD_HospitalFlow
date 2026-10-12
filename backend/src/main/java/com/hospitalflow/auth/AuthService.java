package com.hospitalflow.auth;

import com.hospitalflow.allocation.Doctor;
import com.hospitalflow.allocation.DoctorRepository;
import com.hospitalflow.auth.dto.AuthResponse;
import com.hospitalflow.auth.dto.LoginRequest;
import com.hospitalflow.auth.dto.RegisterRequest;
import com.hospitalflow.auth.dto.StaffUserDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final DoctorRepository doctorRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            DoctorRepository doctorRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        User user = createUserInternal(req);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthResponse(token, user.getRole().name(), user.getName());
    }

    @Transactional
    public StaffUserDto createStaffUser(RegisterRequest req) {
        User user = createUserInternal(req);
        return toStaffUserDto(user);
    }

    private User createUserInternal(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + req.getEmail());
        }
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        // If the new staff member is a DOCTOR, immediately seed their Doctor entity
        if (savedUser.getRole() == User.Role.DOCTOR) {
            doctorRepository.findByUserId(savedUser.getId()).orElseGet(() -> {
                Doctor d = new Doctor();
                d.setUserId(savedUser.getId());
                d.setDepartment("General OPD");
                d.setAvailabilityStatus(Doctor.AvailabilityStatus.AVAILABLE);
                return doctorRepository.save(d);
            });
        }

        return savedUser;
    }

    public List<StaffUserDto> getAllStaff() {
        return userRepository.findAll().stream()
                .map(this::toStaffUserDto)
                .collect(Collectors.toList());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthResponse(token, user.getRole().name(), user.getName());
    }

    private StaffUserDto toStaffUserDto(User u) {
        return new StaffUserDto(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                u.getCreatedAt()
        );
    }
}
