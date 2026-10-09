package com.bilangieri.agendamento.service.repository;

import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

// Filtros combináveis da listagem de serviços
public final class ServiceSpecifications {

    private ServiceSpecifications() {
    }

    // Catálogo visível aos clientes: mesma regra de Service.isBookable()
    public static Specification<Service> bookable() {
        return (root, query, cb) -> {
            Join<Object, Object> professional = root.join("professional");
            return cb.and(
                    cb.isTrue(root.get("active")),
                    cb.isTrue(professional.get("active")),
                    cb.equal(professional.get("approvalStatus"), ApprovalStatus.APPROVED)
            );
        };
    }

    public static Specification<Service> ofProfessional(Long professionalId) {
        return (root, query, cb) -> cb.equal(root.get("professional").get("id"), professionalId);
    }

    public static Specification<Service> inCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Service> nameContains(String text) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + text.trim().toLowerCase() + "%");
    }
}
