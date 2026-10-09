package com.bilangieri.agendamento.service.repository;

import com.bilangieri.agendamento.service.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    // Útil para validar se o prestador já tem um serviço com o mesmo nome
    Optional<Service> findByNameAndProfessionalId(String name, Long professionalId);

    List<Service> findByProfessionalId(Long professionalId);

    // Catálogo visível aos clientes: serviços ativos de prestadores ativos e aprovados
    @Query("""
        SELECT s FROM Service s
        WHERE s.active = true
        AND s.professional.active = true
        AND s.professional.approvalStatus = com.bilangieri.agendamento.user.entity.ApprovalStatus.APPROVED
    """)
    List<Service> findAvailable();

    // Catálogo de um prestador específico, com as mesmas regras de visibilidade de findAvailable
    @Query("""
        SELECT s FROM Service s
        WHERE s.active = true
        AND s.professional.id = :professionalId
        AND s.professional.active = true
        AND s.professional.approvalStatus = com.bilangieri.agendamento.user.entity.ApprovalStatus.APPROVED
    """)
    List<Service> findAvailableByProfessionalId(@Param("professionalId") Long professionalId);
}
