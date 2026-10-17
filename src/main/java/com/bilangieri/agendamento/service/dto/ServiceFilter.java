package com.bilangieri.agendamento.service.dto;

import io.swagger.v3.oas.annotations.Parameter;

// Filtros opcionais e combináveis de GET /api/services
public record ServiceFilter(
        @Parameter(description = "Só os serviços deste prestador (ignorado para PROFESSIONAL)")
        Long professionalId,

        @Parameter(description = "Só os serviços desta categoria")
        Long categoryId,

        @Parameter(description = "Parte do nome do serviço, sem distinguir maiúsculas. "
                + "Mantido por compatibilidade: para pesquisar, use `q`", example = "corte")
        String name,

        @Parameter(description = "Pesquisa recomendada: texto livre procurado no nome do serviço ou no nome do "
                + "prestador, sem distinguir maiúsculas nem acentos (\"depilacao\" encontra \"Depilação\"). "
                + "Os caracteres % e _ são procurados literalmente", example = "ana")
        String q
) {
}
