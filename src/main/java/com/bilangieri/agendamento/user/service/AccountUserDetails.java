package com.bilangieri.agendamento.user.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;

// Conta autenticável, com o instante a partir do qual os tokens são válidos (mudança de email/password ou eliminação)
public class AccountUserDetails extends User {

    private final Instant credentialsChangedAt;

    public AccountUserDetails(String username, String password, boolean enabled,
                              Collection<? extends GrantedAuthority> authorities, Instant credentialsChangedAt) {
        super(username, password, enabled, true, true, true, authorities);
        this.credentialsChangedAt = credentialsChangedAt;
    }

    /**
     * O iat do JWT tem precisão de segundos (arredondado para baixo), por isso compara com o instante da mudança
     * truncado ao segundo: o token emitido logo a seguir à mudança, no mesmo segundo, continua válido.
     */
    public boolean acceptsTokenIssuedAt(Date issuedAt) {
        if (credentialsChangedAt == null) {
            return true;
        }
        return issuedAt != null && !issuedAt.toInstant().isBefore(credentialsChangedAt.truncatedTo(ChronoUnit.SECONDS));
    }
}
