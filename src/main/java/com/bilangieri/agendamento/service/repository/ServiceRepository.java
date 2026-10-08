package com.bilangieri.agendamento.service.repository;

import com.bilangieri.agendamento.service.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    // Útil para validar se já existe um serviço com o mesmo nome
    Optional<Service> findByName(String name);
}