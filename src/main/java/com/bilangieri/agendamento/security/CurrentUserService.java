package com.bilangieri.agendamento.security;

import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Acesso ao utilizador autenticado no pedido atual
@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public String getEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Utilizador não autenticado.");
        }
        return authentication.getName();
    }

    public User getUser() {
        return userRepository.findByEmail(getEmail())
                .orElseThrow(() -> new AccessDeniedException("Utilizador não encontrado."));
    }

    public boolean hasRole(Role role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + role.name()).equals(authority.getAuthority()));
    }
}
