package com.bilangieri.agendamento.auth.controller;

import com.bilangieri.agendamento.auth.dto.AuthResponse;
import com.bilangieri.agendamento.auth.dto.LoginRequest;
import com.bilangieri.agendamento.auth.dto.ProfessionalRegisterRequest;
import com.bilangieri.agendamento.auth.dto.RegisterRequest;
import com.bilangieri.agendamento.auth.service.AuthService;
import com.bilangieri.agendamento.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = OpenApiConfig.TAG_AUTH)
@SecurityRequirements // Endpoints públicos: não pedem token
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registar cliente", description = "Cria a conta e o registo de cliente; pode fazer login de imediato.")
    @ApiResponse(responseCode = "201", description = "Cliente registado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já registado")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/register/professional")
    @Operation(summary = "Registar prestador",
            description = "A conta fica `PENDING` e só pode fazer login depois de aprovada por um ADMIN.")
    @ApiResponse(responseCode = "202", description = "Registo recebido, a aguardar aprovação")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já registado")
    public ResponseEntity<Void> registerProfessional(
            @Valid @RequestBody ProfessionalRegisterRequest request
    ) {
        authService.registerProfessional(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Devolve o token JWT a usar no header `Authorization: Bearer <token>`.")
    @ApiResponse(responseCode = "200", description = "Autenticado")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    @ApiResponse(responseCode = "403", description = "Prestador pendente, rejeitado ou conta desativada")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}
