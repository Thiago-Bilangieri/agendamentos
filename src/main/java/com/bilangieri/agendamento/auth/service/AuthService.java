
package com.bilangieri.agendamento.auth.service;

import com.bilangieri.agendamento.auth.dto.AuthResponse;
import com.bilangieri.agendamento.auth.dto.LoginRequest;
import com.bilangieri.agendamento.auth.dto.ProfessionalRegisterRequest;
import com.bilangieri.agendamento.auth.dto.RegisterRequest;
import com.bilangieri.agendamento.customer.entity.Customer;
import com.bilangieri.agendamento.customer.repository.CustomerRepository;
import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(RegisterRequest request) {

        validateEmailAvailable(request.email());

        User user = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .approvalStatus(ApprovalStatus.APPROVED)
                .active(true)
                .build());

        // Se o ADMIN já tinha criado um cliente com este email, liga-o à nova conta
        Customer customer = customerRepository.findByEmail(request.email())
                .orElseGet(() -> Customer.builder()
                        .name(request.name())
                        .email(request.email())
                        .phone(request.phone())
                        .build());

        if (customer.getUser() != null) {
            throw new BusinessException("Já existe uma conta associada a este cliente.");
        }

        customer.setUser(user);
        customerRepository.save(customer);
    }

    @Transactional
    public void registerProfessional(ProfessionalRegisterRequest request) {

        validateEmailAvailable(request.email());

        userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.PROFESSIONAL)
                .approvalStatus(ApprovalStatus.PENDING)
                .active(true)
                .build());
    }

    private void validateEmailAvailable(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Já existe um utilizador com este email.");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );
        } catch (DisabledException e) {
            throw disabledAccountException(request);
        }

        User user = userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow(() -> new UsernameNotFoundException(
                "Utilizador não encontrado"
        ));

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getRole().name()
        );
    }

    // Explica porque a conta está bloqueada, mas só a quem souber a password
    private RuntimeException disabledAccountException(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElse(null);

        if (user == null) {
            return new BadCredentialsException("Credenciais inválidas");
        }

        return switch (user.getApprovalStatus()) {
            case PENDING -> new AccessDeniedException("O seu registo de prestador aguarda aprovação.");
            case REJECTED -> new AccessDeniedException("O seu registo de prestador foi rejeitado.");
            case APPROVED -> new AccessDeniedException("A sua conta está desativada.");
        };
    }
}
