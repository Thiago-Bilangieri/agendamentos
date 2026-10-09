package com.bilangieri.agendamento.category;

import com.bilangieri.agendamento.IntegrationTest;
import com.bilangieri.agendamento.category.repository.ServiceCategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryIntegrationTest extends IntegrationTest {

    @Autowired
    private ServiceCategoryRepository categoryRepository;

    private Long categoryId(String name) {
        return categoryRepository.findByNameIgnoreCase(name).orElseThrow().getId();
    }

    @Test
    void everyRoleCanListCategories() throws Exception {
        for (String email : new String[]{JOAO, ANA, ADMIN}) {
            mockMvc.perform(get("/api/categories").header("Authorization", bearer(email)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", hasItems("Cabelo", "Barbearia", "Estética")));
        }
    }

    @Test
    void adminCreatesAndRenamesCategory() throws Exception {
        mockMvc.perform(post("/api/categories").header("Authorization", bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Unhas", "description": "Manicure e pedicure"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Unhas"));

        mockMvc.perform(put("/api/categories/{id}", categoryId("Unhas")).header("Authorization", bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Manicure"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Manicure"));
    }

    @Test
    void duplicateNameIsRejectedIgnoringCase() throws Exception {
        mockMvc.perform(post("/api/categories").header("Authorization", bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "cabelo"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminManagesCategories() throws Exception {
        mockMvc.perform(post("/api/categories").header("Authorization", bearer(ANA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Unhas"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void categoryInUseCannotBeDeleted() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", categoryId("Cabelo")).header("Authorization", bearer(ADMIN)))
                .andExpect(status().isConflict());
    }

    @Test
    void unusedCategoryCanBeDeleted() throws Exception {
        mockMvc.perform(post("/api/categories").header("Authorization", bearer(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Temporária"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/categories/{id}", categoryId("Temporária")).header("Authorization", bearer(ADMIN)))
                .andExpect(status().isNoContent());
    }

    @Test
    void servicesCanBeFilteredByCategory() throws Exception {
        mockMvc.perform(get("/api/services").param("categoryId", categoryId("Barbearia").toString())
                        .header("Authorization", bearer(JOAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].categoryName", everyItem(is("Barbearia"))))
                // O serviço inativo da categoria continua escondido do cliente
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void categoryAndNameFiltersCombine() throws Exception {
        mockMvc.perform(get("/api/services")
                        .param("categoryId", categoryId("Barbearia").toString())
                        .param("name", "CORTE")
                        .header("Authorization", bearer(JOAO)))
                .andExpect(jsonPath("$.content[*].name", hasItems("Corte Masculino", "Corte + Barba")))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void professionalCreatesServiceInACategory() throws Exception {
        mockMvc.perform(post("/api/services").header("Authorization", bearer(ANA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Penteado", "price": 30.00, "durationMinutes": 50, "categoryId": %d}
                                """.formatted(categoryId("Cabelo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryName").value("Cabelo"));
    }

    @Test
    void unknownCategoryOnServiceIsNotFound() throws Exception {
        mockMvc.perform(post("/api/services").header("Authorization", bearer(ANA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Penteado", "price": 30.00, "durationMinutes": 50, "categoryId": 999999}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void serviceWithoutCategoryIsStillListed() throws Exception {
        mockMvc.perform(get("/api/services").param("name", "consulta").header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content[*].name", hasItem("Consulta Geral")))
                .andExpect(jsonPath("$.content[0].categoryId").doesNotExist());
    }
}
