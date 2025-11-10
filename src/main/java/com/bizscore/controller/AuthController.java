package com.bizscore.controller;

import com.bizscore.dto.request.LoginRequest;
import com.bizscore.dto.response.JwtResponse;
import com.bizscore.entity.User;
import com.bizscore.repository.UserRepository;
import com.bizscore.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtService.generateToken(userDetails);

            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(authority -> authority.getAuthority())
                    .orElse("ROLE_USER");

            JwtResponse response = new JwtResponse(jwt, userDetails.getUsername(), role);
            log.info("User logged in successfully: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Invalid login attempt for user: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody LoginRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Registration failed - username already exists: {}", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username already exists");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(generateEmail(registerRequest.getUsername()));
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(savedUser.getUsername())
                .password(savedUser.getPassword())
                .authorities("ROLE_" + savedUser.getRole())
                .build();

        String jwt = jwtService.generateToken(userDetails);
        JwtResponse response = new JwtResponse(jwt, savedUser.getUsername(), "ROLE_" + savedUser.getRole());

        log.info("User registered successfully: {}", registerRequest.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private String generateEmail(String username) {
        return username + "@example.com";
    }
}