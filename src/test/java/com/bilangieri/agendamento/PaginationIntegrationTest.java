package com.bilangieri.agendamento;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaginationIntegrationTest extends IntegrationTest {

    @Test
    void listsAreReturnedAsPages() throws Exception {
        mockMvc.perform(get("/api/services").param("size", "5").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(12))
                .andExpect(jsonPath("$.page.totalPages").value(3));
    }

    @Test
    void lastPageHasTheRemainingItems() throws Exception {
        mockMvc.perform(get("/api/services").param("size", "5").param("page", "2")
                        .header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void servicesAreSortedByNameByDefaultAndSortCanBeChanged() throws Exception {
        mockMvc.perform(get("/api/services").header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content[0].name").value("Barba"));

        mockMvc.perform(get("/api/services").param("sort", "price,desc").header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content[0].name").value("Coloração"));
    }

    @Test
    void defaultPageSizeComesFromConfiguration() throws Exception {
        mockMvc.perform(get("/api/appointments").header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.content", hasSize(17)));
    }

    @Test
    void pageSizeIsCapped() throws Exception {
        mockMvc.perform(get("/api/customers").param("size", "1000").header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void appointmentsAreSortedByStartTime() throws Exception {
        mockMvc.perform(get("/api/appointments").param("size", "1").param("sort", "startAt,desc")
                        .header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page.totalElements").value(17));
    }

    @Test
    void professionalsListIsPaged() throws Exception {
        mockMvc.perform(get("/api/admin/professionals").param("status", "APPROVED")
                        .header("Authorization", bearer(ADMIN)))
                .andExpect(jsonPath("$.content[0].name").value("Ana Ribeiro"))
                .andExpect(jsonPath("$.page.totalElements").value(5));
    }

    @Test
    void unknownSortPropertyIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/services").param("sort", "doesNotExist").header("Authorization", bearer(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Não é possível ordenar por 'doesNotExist'."));
    }
}
