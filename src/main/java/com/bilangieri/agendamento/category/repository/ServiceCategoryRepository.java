package com.bilangieri.agendamento.category.repository;

import com.bilangieri.agendamento.category.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    Optional<ServiceCategory> findByNameIgnoreCase(String name);
}
