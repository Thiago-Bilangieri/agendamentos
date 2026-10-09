package com.bilangieri.agendamento.config;

import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro ADMIN ao arrancar, a partir de ADMIN_EMAIL / ADMIN_PASSWORD.
 * Fora do perfil dev não há utilizadores de teste, por isso é assim que um ambiente novo ganha o seu administrador.
 * Se o email já existir não faz nada (nunca altera a password de uma conta existente).
 */
@Slf4j
@Component
public class AdminBootstrap implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${api.admin.email:}") String email,
            @Value("${api.admin.password:}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            if (!userRepository.existsByRole(Role.ADMIN)) {
                log.warn("Não existe nenhum ADMIN. Defina ADMIN_EMAIL e ADMIN_PASSWORD para criar o primeiro.");
            }
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD tem de ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        userRepository.save(User.builder()
                .name("Administrador")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build());
        log.info("ADMIN inicial criado: {}", email);
    }
}
