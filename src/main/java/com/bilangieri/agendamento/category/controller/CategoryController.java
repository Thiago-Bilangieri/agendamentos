package com.bilangieri.agendamento.category.controller;

import com.bilangieri.agendamento.category.dto.CategoryRequest;
import com.bilangieri.agendamento.category.dto.CategoryResponse;
import com.bilangieri.agendamento.category.service.CategoryService;
import com.bilangieri.agendamento.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.TAG_CATEGORIES)
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Listar categorias", description = "**Todos os perfis.** Por ordem alfabética.")
    public ResponseEntity<List<CategoryResponse>> findAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de uma categoria", description = "**Todos os perfis.**")
    @ApiResponse(responseCode = "404", description = "Categoria inexistente")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Criar categoria", description = "**ADMIN.**")
    @ApiResponse(responseCode = "201", description = "Categoria criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou nome já existente")
    public ResponseEntity<CategoryResponse> create(@RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar categoria", description = "**ADMIN.**")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou nome já existente")
    @ApiResponse(responseCode = "404", description = "Categoria inexistente")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover categoria", description = "**ADMIN.** Categorias em uso não podem ser removidas.")
    @ApiResponse(responseCode = "204", description = "Removida")
    @ApiResponse(responseCode = "404", description = "Categoria inexistente")
    @ApiResponse(responseCode = "409", description = "A categoria tem serviços associados")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
