package com.bilangieri.agendamento.user;

import com.bilangieri.agendamento.IntegrationTest;
import com.bilangieri.agendamento.appointment.entity.Appointment;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatus;
import com.bilangieri.agendamento.appointment.entity.AppointmentStatusHistory;
import com.bilangieri.agendamento.appointment.repository.AppointmentStatusHistoryRepository;
import com.bilangieri.agendamento.customer.entity.Customer;
import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Os testes que erram passwords ou mudam credenciais usam contas criadas só para eles:
 * o contador de tentativas falhadas vive em memória e não é revertido com a transação.
 */
class UserProfileIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "secret123";

    @Value("${api.security.token.secret}")
    private String tokenSecret;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AppointmentStatusHistoryRepository statusHistoryRepository;

    private String registerCustomer(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Cliente Teste", "email": "%s", "password": "%s", "phone": "912000000"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
        return email;
    }

    private User saveProfessional(String email) {
        return userRepository.save(User.builder()
                .name("Prestador Teste")
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .role(Role.PROFESSIONAL)
                .approvalStatus(ApprovalStatus.APPROVED)
                .active(true)
                .build());
    }

    // Token de uma sessão iniciada antes (iat 10 s no passado), como o de outro dispositivo
    private String olderSession(String email) {
        Date issuedAt = Date.from(Instant.now().minusSeconds(10));
        return "Bearer " + Jwts.builder()
                .subject(email)
                .issuedAt(issuedAt)
                .expiration(new Date(issuedAt.getTime() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)));
    }

    private ResultActions getMe(String authorization) throws Exception {
        return mockMvc.perform(get("/api/users/me").header("Authorization", authorization));
    }

    private ResultActions putMe(String authorization, String body) throws Exception {
        return mockMvc.perform(put("/api/users/me")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions changeEmail(String authorization, String newEmail, String currentPassword) throws Exception {
        return mockMvc.perform(patch("/api/users/me/email")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"newEmail": "%s", "currentPassword": "%s"}
                        """.formatted(newEmail, currentPassword)));
    }

    private ResultActions changePassword(String authorization, String currentPassword, String newPassword) throws Exception {
        return mockMvc.perform(patch("/api/users/me/password")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(currentPassword, newPassword)));
    }

    private ResultActions deleteMe(String authorization, String currentPassword) throws Exception {
        return mockMvc.perform(delete("/api/users/me")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s"}
                        """.formatted(currentPassword)));
    }

    private static String tokenOf(ResultActions result) throws Exception {
        return "Bearer " + JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.token");
    }

    // ---------- GET / PUT /api/users/me ----------

    @Test
    void customerSeesProfileWithPhone() throws Exception {
        getMe(bearer(JOAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user(JOAO).getId()))
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value(JOAO))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.phone").value("912345001"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void professionalAndAdminSeeProfileWithoutPhone() throws Exception {
        getMe(bearer(ANA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROFESSIONAL"))
                .andExpect(jsonPath("$.phone").value(nullValue()));

        getMe(bearer(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.phone").value(nullValue()));
    }

    @Test
    void profileRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerUpdatesNameAndPhoneInUserAndCustomer() throws Exception {
        putMe(bearer(JOAO), """
                {"name": "  João Pedro Silva  ", "phone": " 919999999 "}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João Pedro Silva"))
                .andExpect(jsonPath("$.phone").value("919999999"));

        assertThat(user(JOAO).getName()).isEqualTo("João Pedro Silva");
        Customer customer = customer(JOAO);
        assertThat(customer.getName()).isEqualTo("João Pedro Silva");
        assertThat(customer.getPhone()).isEqualTo("919999999");
    }

    @Test
    void customerMustSendPhone() throws Exception {
        putMe(bearer(JOAO), """
                {"name": "João Silva", "phone": "  "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("O telefone é obrigatório"));
    }

    @Test
    void professionalUpdatesNameAndPhoneIsIgnored() throws Exception {
        putMe(bearer(ANA), """
                {"name": "Ana R. Ribeiro"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana R. Ribeiro"))
                .andExpect(jsonPath("$.phone").value(nullValue()));

        assertThat(user(ANA).getName()).isEqualTo("Ana R. Ribeiro");

        putMe(bearer(ADMIN), """
                {"name": "Administrador", "phone": "912000000"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(nullValue()));
    }

    @Test
    void nameIsRequiredAndLimitedTo100Characters() throws Exception {
        putMe(bearer(ANA), """
                {"name": "   "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("O nome é obrigatório"));

        putMe(bearer(ANA), """
                {"name": "%s"}
                """.formatted("a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("O nome não pode ter mais de 100 caracteres"));
    }

    // ---------- PATCH /api/users/me/email ----------

    @Test
    void changingEmailIssuesNewTokenAndRevokesOlderSessions() throws Exception {
        String email = registerCustomer("email.change@email.com");
        String oldSession = olderSession(email);

        ResultActions result = changeEmail(oldSession, "  Email.Novo@Email.com ", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("email.novo@email.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.token").isNotEmpty());
        String newSession = tokenOf(result);

        getMe(newSession)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("email.novo@email.com"));
        getMe(oldSession).andExpect(status().isUnauthorized());

        assertThat(customerRepository.findByUserEmail("email.novo@email.com").orElseThrow().getEmail())
                .isEqualTo("email.novo@email.com");
        login("email.novo@email.com", PASSWORD).andExpect(status().isOk());
        login(email, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void professionalChangesEmail() throws Exception {
        saveProfessional("prof.email@email.com");

        changeEmail(bearer("prof.email@email.com"), "prof.novo@email.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PROFESSIONAL"));

        assertThat(userRepository.findByEmail("prof.novo@email.com")).isPresent();
    }

    @Test
    void cannotChangeToAnEmailAlreadyInUse() throws Exception {
        String email = registerCustomer("email.taken@email.com");

        changeEmail(bearer(email), MARIA.toUpperCase(), PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.detail").value("Já existe um utilizador com este email."));
    }

    // Rita Pereira é uma cliente criada pelo ADMIN, sem conta: o email está ocupado na tabela customers
    @Test
    void customerCannotTakeTheEmailOfACustomerWithoutAccount() throws Exception {
        String email = registerCustomer("email.rita@email.com");

        changeEmail(bearer(email), "rita.pereira@email.com", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Já existe um utilizador com este email."));
    }

    @Test
    void changingEmailWithWrongPasswordIsAFieldError() throws Exception {
        String email = registerCustomer("email.wrong@email.com");

        changeEmail(bearer(email), "outro@email.com", "errada123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors.currentPassword").value("A password atual está incorreta."));

        assertThat(user(email).getCredentialsChangedAt()).isNull();
    }

    @Test
    void newEmailMustBeValidAndDifferent() throws Exception {
        String email = registerCustomer("email.same@email.com");

        changeEmail(bearer(email), "not-an-email", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newEmail").value("Email inválido"));

        changeEmail(bearer(email), email.toUpperCase(), PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newEmail").value("O novo email é igual ao atual."));
    }

    // ---------- PATCH /api/users/me/password ----------

    @Test
    void changingPasswordIssuesNewTokenAndRevokesOlderSessions() throws Exception {
        String email = registerCustomer("password.change@email.com");
        String oldSession = olderSession(email);

        ResultActions result = changePassword(oldSession, PASSWORD, "novaPassword1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.token").isNotEmpty());

        getMe(tokenOf(result)).andExpect(status().isOk());
        getMe(oldSession).andExpect(status().isUnauthorized());
        login(email, "novaPassword1").andExpect(status().isOk());
        login(email, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void changingPasswordWithWrongCurrentPasswordIsAFieldError() throws Exception {
        String email = registerCustomer("password.wrong@email.com");

        changePassword(bearer(email), "errada123", "novaPassword1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").value("A password atual está incorreta."));
    }

    @Test
    void newPasswordMustFollowThePolicyAndDifferFromTheCurrent() throws Exception {
        String email = registerCustomer("password.policy@email.com");

        changePassword(bearer(email), PASSWORD, "password")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").value(
                        "A password deve ter entre 8 e 72 caracteres e incluir pelo menos uma letra e um número."));

        changePassword(bearer(email), PASSWORD, PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").value("A nova password tem de ser diferente da atual."));
    }

    @Test
    void repeatedWrongCurrentPasswordsAreRateLimitedLikeTheLogin() throws Exception {
        String email = registerCustomer("password.brute@email.com");
        String session = bearer(email);

        for (int i = 0; i < 5; i++) {
            changePassword(session, "errada123", "novaPassword1").andExpect(status().isBadRequest());
        }

        changePassword(session, PASSWORD, "novaPassword1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Demasiados pedidos"));
        // Partilha o contador com o login
        login(email, PASSWORD).andExpect(status().isTooManyRequests());
    }

    // ---------- DELETE /api/users/me ----------

    @Test
    void deletingCustomerAccountCancelsUpcomingAppointmentsAndAnonymizes() throws Exception {
        String email = registerCustomer("delete.customer@email.com");
        User account = user(email);
        Customer customer = customer(email);
        customer.setNotes("Notas pessoais");
        Service corte = service(ANA, "Corte Feminino");
        Appointment upcoming = saveAppointment(customer, corte, nextYearAt(10, 0), AppointmentStatus.CONFIRMED);
        Appointment past = saveAppointment(customer, corte, now().minusDays(60).withHour(10).withMinute(0),
                AppointmentStatus.COMPLETED);
        String session = bearer(email);

        deleteMe(session, PASSWORD).andExpect(status().isNoContent());

        assertThat(appointmentRepository.findById(upcoming.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointmentRepository.findById(past.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.COMPLETED);
        List<AppointmentStatusHistory> history =
                statusHistoryRepository.findByAppointmentIdOrderByChangedAtAscIdAsc(upcoming.getId());
        assertThat(history).singleElement().satisfies(entry -> {
            assertThat(entry.getFromStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
            assertThat(entry.getToStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(entry.getChangedByRole()).isEqualTo(Role.CUSTOMER);
            assertThat(entry.getChangedBy().getId()).isEqualTo(account.getId());
            assertThat(entry.getReason()).isEqualTo("Conta eliminada pelo cliente");
        });

        String anonymizedEmail = "deleted-" + account.getId() + "@deleted.invalid";
        Customer anonymizedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(anonymizedCustomer.getName()).isEqualTo("Cliente removido");
        assertThat(anonymizedCustomer.getEmail()).isEqualTo(anonymizedEmail);
        assertThat(anonymizedCustomer.getPhone()).isEqualTo("-");
        assertThat(anonymizedCustomer.getNotes()).isNull();
        User anonymizedUser = userRepository.findById(account.getId()).orElseThrow();
        assertThat(anonymizedUser.getName()).isEqualTo("Cliente removido");
        assertThat(anonymizedUser.getEmail()).isEqualTo(anonymizedEmail);
        assertThat(anonymizedUser.getActive()).isFalse();
        assertThat(passwordEncoder.matches(PASSWORD, anonymizedUser.getPassword())).isFalse();

        getMe(session).andExpect(status().isUnauthorized());
        login(email, PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void professionalWithUpcomingAppointmentsCannotDeleteTheAccount() throws Exception {
        saveAppointment(customer(JOAO), service(ANA, "Corte Feminino"), nextYearAt(10, 0), AppointmentStatus.SCHEDULED);

        deleteMe(bearer(ANA), "password")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cancele primeiro as marcações futuras."));

        assertThat(user(ANA).getActive()).isTrue();
    }

    @Test
    void deletingProfessionalAccountDeactivatesServicesAndAnonymizes() throws Exception {
        User professional = saveProfessional("delete.professional@email.com");
        Service massage = serviceRepository.save(Service.builder()
                .name("Massagem")
                .professional(professional)
                .price(new BigDecimal("40.00"))
                .durationMinutes(60)
                .build());

        deleteMe(bearer("delete.professional@email.com"), PASSWORD).andExpect(status().isNoContent());

        assertThat(serviceRepository.findById(massage.getId()).orElseThrow().getActive()).isFalse();
        User anonymized = userRepository.findById(professional.getId()).orElseThrow();
        assertThat(anonymized.getName()).isEqualTo("Profissional removido");
        assertThat(anonymized.getEmail()).isEqualTo("deleted-" + professional.getId() + "@deleted.invalid");
        assertThat(anonymized.getActive()).isFalse();
        login("delete.professional@email.com", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCannotDeleteTheirOwnAccount() throws Exception {
        deleteMe(bearer(ADMIN), "password")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Um administrador não pode eliminar a própria conta."));

        assertThat(user(ADMIN).getActive()).isTrue();
    }

    @Test
    void deletingWithWrongPasswordKeepsTheAccount() throws Exception {
        String email = registerCustomer("delete.wrong@email.com");

        deleteMe(bearer(email), "errada123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currentPassword").value("A password atual está incorreta."));

        assertThat(user(email).getActive()).isTrue();
    }
}
