package com.bilangieri.agendamento.user;

import java.util.Locale;

// O email identifica a conta: "Joao@Email.com" e "joao@email.com" são o mesmo utilizador.
// Todas as entradas passam por aqui; a migração V10 garante o mesmo na base de dados.
public final class Emails {

    private Emails() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
