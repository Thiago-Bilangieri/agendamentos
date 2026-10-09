package com.bilangieri.agendamento.category.service;

import com.bilangieri.agendamento.category.dto.CategoryRequest;
import com.bilangieri.agendamento.category.dto.CategoryResponse;
import com.bilangieri.agendamento.category.entity.ServiceCategory;
import com.bilangieri.agendamento.category.repository.ServiceCategoryRepository;
import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.exception.ConflictException;
import com.bilangieri.agendamento.exception.NotFoundException;
import com.bilangieri.agendamento.service.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryResponse.fromEntity(getCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        checkNameIsFree(request.name(), null);

        ServiceCategory category = ServiceCategory.builder()
                .name(request.name().trim())
                .description(request.description())
                .build();

        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        ServiceCategory category = getCategory(id);
        checkNameIsFree(request.name(), id);

        category.setName(request.name().trim());
        category.setDescription(request.description());

        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        ServiceCategory category = getCategory(id);

        if (serviceRepository.existsByCategoryId(id)) {
            throw new ConflictException("Esta categoria tem serviços associados e não pode ser removida.");
        }

        categoryRepository.delete(category);
    }

    // Usado pelos serviços para validar o categoryId recebido
    @Transactional(readOnly = true)
    public ServiceCategory getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria não encontrada com o ID: " + id));
    }

    private void checkNameIsFree(String name, Long currentId) {
        categoryRepository.findByNameIgnoreCase(name.trim())
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new BusinessException("Já existe uma categoria com este nome.");
                });
    }
}
