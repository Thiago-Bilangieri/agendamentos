package com.bilangieri.agendamento.user.service;

import com.bilangieri.agendamento.appointment.service.AppointmentService;
import com.bilangieri.agendamento.auth.dto.AuthResponse;
import com.bilangieri.agendamento.auth.service.AuthService;
import com.bilangieri.agendamento.auth.service.JwtService;
import com.bilangieri.agendamento.auth.service.LoginAttemptService;
import com.bilangieri.agendamento.customer.entity.Customer;
import com.bilangieri.agendamento.customer.repository.CustomerRepository;
import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.exception.ConflictException;
import com.bilangieri.agendamento.exception.FieldValidationException;
import com.bilangieri.agendamento.security.CurrentUserService;
import com.bilangieri.agendamento.service.repository.ServiceRepository;
import com.bilangieri.agendamento.user.dto.AccountDeleteRequest;
import com.bilangieri.agendamento.user.dto.EmailChangeRequest;
import com.bilangieri.agendamento.user.dto.PasswordChangeRequest;
import com.bilangieri.agendamento.user.dto.ProfileUpdateRequest;
import com.bilangieri.agendamento.user.dto.UserProfileResponse;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;

/**
 * "O meu perfil": o utilizador autenticado lê e altera os seus dados, muda o email ou a password e elimina a conta.
 * O cliente tem nome e email duplicados em Customer, por isso cada mudança atualiza os dois na mesma transação.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    static final String CUSTOMER_DELETED_REASON = "Conta eliminada pelo cliente";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CurrentUserService currentUserService;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final AppointmentService appointmentService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UserProfileResponse get() {
        User user = currentUserService.getUser();
        return UserProfileResponse.of(user, customerOf(user).orElse(null));
    }

    @Transactional
    public UserProfileResponse update(ProfileUpdateRequest request) {
        User user = currentUserService.getUser();
        Customer customer = customerOf(user).orElse(null);

        // O telefone só existe no registo de cliente; nos outros perfis é ignorado
        if (customer != null && (request.phone() == null || request.phone().isBlank())) {
            throw new FieldValidationException("phone", "O telefone é obrigatório");
        }

        user.setName(request.name());
        if (customer != null) {
            customer.setName(request.name());
            customer.setPhone(request.phone());
        }
        return UserProfileResponse.of(user, customer);
    }

    @Transactional
    public AuthResponse changeEmail(EmailChangeRequest request) {
        User user = currentUserService.getUser();
        verifyCurrentPassword(user, request.currentPassword());

        String newEmail = request.newEmail();
        if (newEmail.equals(user.getEmail())) {
            throw new FieldValidationException("newEmail", "O novo email é igual ao atual.");
        }
        authService.validateEmailAvailable(newEmail);

        Optional<Customer> customer = customerOf(user);
        // Um cliente criado pelo ADMIN (sem conta) também ocupa o email na tabela customers
        if (customer.isPresent() && customerRepository.findByEmail(newEmail).isPresent()) {
            throw new BusinessException(AuthService.EMAIL_IN_USE);
        }

        user.setEmail(newEmail);
        customer.ifPresent(c -> c.setEmail(newEmail));
        return newSession(user);
    }

    @Transactional
    public AuthResponse changePassword(PasswordChangeRequest request) {
        User user = currentUserService.getUser();
        verifyCurrentPassword(user, request.currentPassword());

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new FieldValidationException("newPassword", "A nova password tem de ser diferente da atual.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        return newSession(user);
    }

    /**
     * Direito ao apagamento (RGPD): os dados pessoais são anonimizados e a conta desativada; os registos de
     * marcações mantêm-se, porque fazem parte do histórico do prestador.
     */
    @Transactional
    public void deleteAccount(AccountDeleteRequest request) {
        User user = currentUserService.getUser();
        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException("Um administrador não pode eliminar a própria conta.");
        }
        verifyCurrentPassword(user, request.currentPassword());

        String anonymizedEmail = "deleted-" + user.getId() + "@deleted.invalid";
        if (user.getRole() == Role.CUSTOMER) {
            // Antes de anonimizar: o histórico regista o utilizador autenticado, que é procurado pelo email atual
            customerOf(user).ifPresent(customer -> {
                appointmentService.cancelUpcomingOfCustomer(customer.getId(), CUSTOMER_DELETED_REASON);
                customer.setName("Cliente removido");
                customer.setEmail(anonymizedEmail);
                customer.setPhone("-");
                customer.setNotes(null);
            });
            anonymize(user, "Cliente removido", anonymizedEmail);
        } else {
            if (appointmentService.hasUpcomingForProfessional(user.getId())) {
                throw new ConflictException("Cancele primeiro as marcações futuras.");
            }
            serviceRepository.findByProfessionalId(user.getId()).forEach(service -> service.setActive(false));
            anonymize(user, "Profissional removido", anonymizedEmail);
        }
    }

    private void anonymize(User user, String name, String email) {
        byte[] randomPassword = new byte[32];
        RANDOM.nextBytes(randomPassword);

        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(Base64.getEncoder().encodeToString(randomPassword)));
        user.setActive(false);
        user.markCredentialsChanged(clock.instant());
    }

    // Os tokens emitidos antes deste instante deixam de valer; o token devolvido mantém a sessão atual
    private AuthResponse newSession(User user) {
        user.markCredentialsChanged(clock.instant());
        return new AuthResponse(jwtService.generateToken(user.getEmail()), user.getEmail(), user.getRole().name());
    }

    // Conta para a proteção contra força bruta tal como o login: quem tem o token não pode adivinhar a password.
    // Uma password errada é um erro do campo (400), não 401, que o front trata como sessão expirada
    private void verifyCurrentPassword(User user, String currentPassword) {
        loginAttemptService.checkAllowed(user.getEmail());
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            loginAttemptService.recordFailure(user.getEmail());
            throw new FieldValidationException("currentPassword", "A password atual está incorreta.");
        }
        loginAttemptService.recordSuccess(user.getEmail());
    }

    private Optional<Customer> customerOf(User user) {
        return user.getRole() == Role.CUSTOMER ? customerRepository.findByUserId(user.getId()) : Optional.empty();
    }
}
