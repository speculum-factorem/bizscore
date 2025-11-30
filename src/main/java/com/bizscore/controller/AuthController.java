package com.bizscore.controller;

import com.bizscore.dto.request.LoginRequest;
import com.bizscore.dto.response.JwtResponse;
import com.bizscore.entity.User;
import com.bizscore.repository.UserRepository;
import com.bizscore.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
@Tag(name = "Authentication API", description = "API для аутентификации и регистрации")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "Аутентификация пользователя", description = "Вход в систему и получение JWT токена")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация"),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Данные для входа: username и password")
            @Valid @RequestBody LoginRequest loginRequest) {

        // Настройка MDC для логирования
        MDC.put("operation", "login");
        MDC.put("username", loginRequest.getUsername());

        try {
            log.info("Попытка аутентификации пользователя: {}", loginRequest.getUsername());
            
            // Аутентификация пользователя через Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // Генерация JWT токена для аутентифицированного пользователя
            String jwt = jwtService.generateToken(userDetails);

            // Извлечение роли пользователя
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(authority -> authority.getAuthority())
                    .orElse("ROLE_USER");

            JwtResponse response = new JwtResponse(jwt, userDetails.getUsername(), role);
            MDC.put("role", role);
            log.info("Пользователь успешно аутентифицирован: {}, роль: {}", loginRequest.getUsername(), role);
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Неудачная попытка входа для пользователя: {}", loginRequest.getUsername());
            MDC.put("authError", "bad_credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } finally {
            MDC.remove("operation");
            MDC.remove("username");
            MDC.remove("role");
            MDC.remove("authError");
        }
    }

    @Operation(summary = "Регистрация пользователя", description = "Создание нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Пользователь успешно создан"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже существует")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Данные для регистрации: username и password")
            @Valid @RequestBody LoginRequest registerRequest) {

        // Настройка MDC для логирования
        MDC.put("operation", "register");
        MDC.put("username", registerRequest.getUsername());

        try {
            log.info("Попытка регистрации нового пользователя: {}", registerRequest.getUsername());

            // Проверка существования пользователя
            if (userRepository.existsByUsername(registerRequest.getUsername())) {
                log.warn("Регистрация не удалась - имя пользователя уже существует: {}", registerRequest.getUsername());
                MDC.put("registrationError", "username_exists");
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Имя пользователя уже существует");
            }

            // Создание нового пользователя
            User user = new User();
            user.setUsername(registerRequest.getUsername());
            // Хеширование пароля перед сохранением
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setEmail(generateEmail(registerRequest.getUsername()));
            user.setRole("USER");

            User savedUser = userRepository.save(user);
            log.debug("Пользователь сохранен в базу данных с ID: {}", savedUser.getId());

            // Создание UserDetails для генерации JWT токена
            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(savedUser.getUsername())
                    .password(savedUser.getPassword())
                    .authorities("ROLE_" + savedUser.getRole())
                    .build();

            // Генерация JWT токена для нового пользователя
            String jwt = jwtService.generateToken(userDetails);
            JwtResponse response = new JwtResponse(jwt, savedUser.getUsername(), "ROLE_" + savedUser.getRole());

            MDC.put("role", "ROLE_USER");
            log.info("Пользователь успешно зарегистрирован: {}", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Ошибка при регистрации пользователя: {}", registerRequest.getUsername(), e);
            MDC.put("registrationError", "internal_error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            MDC.remove("operation");
            MDC.remove("username");
            MDC.remove("role");
            MDC.remove("registrationError");
        }
    }

    // Генерация email адреса на основе имени пользователя
    private String generateEmail(String username) {
        return username + "@example.com";
    }
}