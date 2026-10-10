package com.bilangieri.agendamento.customer.controller;

import com.bilangieri.agendamento.config.OpenApiConfig;
import com.bilangieri.agendamento.customer.dto.CustomerCreateRequest;
import com.bilangieri.agendamento.customer.dto.CustomerResponse;
import com.bilangieri.agendamento.customer.dto.CustomerUpdateRequest;
import com.bilangieri.agendamento.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_CUSTOMERS)
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Criar cliente sem conta", description = "**ADMIN.** Ex.: cliente registado ao balcão.")
    @ApiResponse(responseCode = "201", description = "Cliente criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já registado")
    public ResponseEntity<CustomerResponse> create(@RequestBody @Valid CustomerCreateRequest request) {
        CustomerResponse response = customerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar clientes", description = "**ADMIN.** Ordenação por omissão: `name`.")
    public ResponseEntity<Page<CustomerResponse>> findAll(
            @ParameterObject @SortDefault(sort = "name") Pageable pageable) {
        return ResponseEntity.ok(customerService.findAll(pageable));
    }

    @GetMapping("/me")
    @Operation(summary = "O meu perfil de cliente", description = "**CUSTOMER.**")
    public ResponseEntity<CustomerResponse> findMe(@Parameter(hidden = true) Authentication authentication) {
        CustomerResponse response = customerService.findByUserEmail(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de um cliente", description = "**ADMIN.**")
    @ApiResponse(responseCode = "404", description = "Cliente inexistente")
    public ResponseEntity<CustomerResponse> findById(@PathVariable Long id) {
        CustomerResponse response = customerService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente", description = "**ADMIN.**")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou email já registado")
    @ApiResponse(responseCode = "404", description = "Cliente inexistente")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid CustomerUpdateRequest request) {
        CustomerResponse response = customerService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover cliente", description = "**ADMIN.** Clientes com agendamentos não podem ser removidos.")
    @ApiResponse(responseCode = "204", description = "Removido")
    @ApiResponse(responseCode = "404", description = "Cliente inexistente")
    @ApiResponse(responseCode = "409", description = "O cliente tem agendamentos associados")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
