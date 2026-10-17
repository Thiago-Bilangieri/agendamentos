package com.bilangieri.agendamento.user.controller;

import com.bilangieri.agendamento.auth.dto.AuthResponse;
import com.bilangieri.agendamento.config.OpenApiConfig;
import com.bilangieri.agendamento.user.dto.AccountDeleteRequest;
import com.bilangieri.agendamento.user.dto.EmailChangeRequest;
import com.bilangieri.agendamento.user.dto.PasswordChangeRequest;
import com.bilangieri.agendamento.user.dto.ProfileUpdateRequest;
import com.bilangieri.agendamento.user.dto.UserProfileResponse;
import com.bilangieri.agendamento.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@Tag(name = OpenApiConfig.TAG_PROFILE)
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @Operation(summary = "O meu perfil", description = "**Todos os perfis.** `phone` só existe para o CUSTOMER (null nos outros).")
    public ResponseEntity<UserProfileResponse> get() {
        return ResponseEntity.ok(userProfileService.get());
    }

    @PutMapping
    @Operation(summary = "Atualizar o meu perfil",
            description = "**Todos os perfis.** `phone` é obrigatório para o CUSTOMER e ignorado nos outros perfis.")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos (`errors.name`, `errors.phone`)")
    public ResponseEntity<UserProfileResponse> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(userProfileService.update(request));
    }

    @PatchMapping("/email")
    @Operation(summary = "Mudar o email",
            description = "**Todos os perfis.** Devolve um token novo, que mantém esta sessão; "
                    + "os tokens emitidos antes deixam de ser aceites (as outras sessões caem).")
    @ApiResponse(responseCode = "200", description = "Email alterado; usar o token novo")
    @ApiResponse(responseCode = "400", description = "Dados inválidos, password atual errada (`errors.currentPassword`) "
            + "ou email já registado")
    @ApiResponse(responseCode = "429", description = "Demasiadas passwords erradas; ver o header Retry-After")
    public ResponseEntity<AuthResponse> changeEmail(@Valid @RequestBody EmailChangeRequest request) {
        return ResponseEntity.ok(userProfileService.changeEmail(request));
    }

    @PatchMapping("/password")
    @Operation(summary = "Mudar a password",
            description = "**Todos os perfis.** A nova password tem de ter 8 a 72 caracteres, com pelo menos uma letra "
                    + "e um número, e ser diferente da atual. Devolve um token novo, que mantém esta sessão; "
                    + "os tokens emitidos antes deixam de ser aceites (as outras sessões caem).")
    @ApiResponse(responseCode = "200", description = "Password alterada; usar o token novo")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou password atual errada "
            + "(`errors.currentPassword`, `errors.newPassword`)")
    @ApiResponse(responseCode = "429", description = "Demasiadas passwords erradas; ver o header Retry-After")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        return ResponseEntity.ok(userProfileService.changePassword(request));
    }

    @DeleteMapping
    @Operation(summary = "Eliminar a minha conta",
            description = "**CUSTOMER e PROFESSIONAL.** Direito ao apagamento (RGPD): os dados pessoais são anonimizados, "
                    + "a conta é desativada e os registos de marcações mantêm-se. As marcações futuras do cliente "
                    + "são canceladas; o prestador tem de cancelar as suas antes, e os seus serviços são desativados.")
    @ApiResponse(responseCode = "204", description = "Conta eliminada; o token deixa de ser aceite")
    @ApiResponse(responseCode = "400", description = "Password atual errada (`errors.currentPassword`) "
            + "ou o utilizador é ADMIN")
    @ApiResponse(responseCode = "409", description = "O prestador ainda tem marcações futuras")
    @ApiResponse(responseCode = "429", description = "Demasiadas passwords erradas; ver o header Retry-After")
    public ResponseEntity<Void> delete(@Valid @RequestBody AccountDeleteRequest request) {
        userProfileService.deleteAccount(request);
        return ResponseEntity.noContent().build();
    }
}
