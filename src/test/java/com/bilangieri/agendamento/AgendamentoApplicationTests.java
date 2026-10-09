package com.bilangieri.agendamento;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AgendamentoApplicationTests {

    @Test
    void contextLoads() {
    }

}
