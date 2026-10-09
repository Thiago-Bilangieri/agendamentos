package com.bilangieri.agendamento.config;

import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(userRepository, passwordEncoder, email, password);
    }

    @Test
    void createsTheFirstAdminWithAnEncodedPassword() {
        when(userRepository.findByEmail("boss@empresa.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("uma-password-forte")).thenReturn("hash");

        bootstrap("boss@empresa.com", "uma-password-forte").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("boss@empresa.com");
        assertThat(saved.getValue().getPassword()).isEqualTo("hash");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getValue().getActive()).isTrue();
        assertThat(saved.getValue().getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    }

    @Test
    void neverTouchesAnExistingAccount() {
        when(userRepository.findByEmail("boss@empresa.com")).thenReturn(Optional.of(new User()));

        bootstrap("boss@empresa.com", "uma-password-forte").run(null);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void doesNothingWhenNotConfigured() {
        bootstrap("", "").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsAWeakPassword() {
        assertThatThrownBy(() -> bootstrap("boss@empresa.com", "123").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");

        verify(userRepository, never()).save(any());
    }
}
