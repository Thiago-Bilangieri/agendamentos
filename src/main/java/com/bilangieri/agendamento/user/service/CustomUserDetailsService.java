package com.bilangieri.agendamento.user.service;

import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Conta desativada ou prestador ainda não aprovado: existe, mas não pode autenticar
    @Override
    public AccountUserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado: " + email));

        return new AccountUserDetails(
                user.getEmail(),
                user.getPassword(),
                Boolean.TRUE.equals(user.getActive()) && user.getApprovalStatus() == ApprovalStatus.APPROVED,
                AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name()),
                user.getCredentialsChangedInstant());
    }
}
