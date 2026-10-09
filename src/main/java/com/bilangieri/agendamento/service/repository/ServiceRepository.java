package com.bilangieri.agendamento.service.repository;

import com.bilangieri.agendamento.service.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long>, JpaSpecificationExecutor<Service> {

    // Útil para validar se o prestador já tem um serviço com o mesmo nome
    Optional<Service> findByNameAndProfessionalId(String name, Long professionalId);

    List<Service> findByProfessionalId(Long professionalId);

    boolean existsByCategoryId(Long categoryId);
}
