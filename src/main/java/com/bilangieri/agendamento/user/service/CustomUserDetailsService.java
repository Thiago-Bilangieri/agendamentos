package com.bilangieri.agendamento.user.service;

import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Conta desativada ou prestador ainda não aprovado: existe, mas não pode autenticar
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(!Boolean.TRUE.equals(user.getActive())
                        || user.getApprovalStatus() != ApprovalStatus.APPROVED)
                .build();
    }
}
