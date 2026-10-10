package com.bilangieri.agendamento.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// Os horários (agendamentos, horário de trabalho) são locais ao negócio e guardados sem fuso.
// O "agora" usado nas regras tem de estar nesse mesmo fuso, não no da JVM (em Docker/CI é UTC).
// Injetar o Clock também permite fixar o tempo nos testes.
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${api.time-zone}") ZoneId zone) {
        return Clock.system(zone);
    }
}
