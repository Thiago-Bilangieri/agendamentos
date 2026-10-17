package com.bilangieri.agendamento.service.repository;

import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.user.entity.ApprovalStatus;
import com.bilangieri.agendamento.user.entity.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

// Filtros combináveis da listagem de serviços
public final class ServiceSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ServiceSpecifications() {
    }

    // Catálogo visível aos clientes: mesma regra de Service.isBookable()
    public static Specification<Service> bookable() {
        return (root, query, cb) -> {
            Join<Service, User> professional = professional(root);
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

    // Filtro antigo `name`: só o nome do serviço, sem distinguir maiúsculas (mas distingue acentos)
    public static Specification<Service> nameContains(String text) {
        String pattern = containsPattern(text.toLowerCase(Locale.ROOT));
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE);
    }

    // Pesquisa livre `q`: nome do serviço OU do prestador, sem distinguir maiúsculas nem acentos
    public static Specification<Service> serviceOrProfessionalNameContains(String text) {
        String pattern = containsPattern(text);
        return (root, query, cb) -> cb.or(
                unaccentLike(cb, root.get("name"), pattern),
                unaccentLike(cb, professional(root).get("name"), pattern)
        );
    }

    // lower(unaccent(coluna)) LIKE lower(unaccent(?)); a função unaccent vem da migração V12.
    // value() envia o padrão como parâmetro (cb.literal escrevê-lo-ia dentro do SQL)
    private static Predicate unaccentLike(CriteriaBuilder cb, Expression<String> column, String pattern) {
        Expression<String> boundPattern = ((HibernateCriteriaBuilder) cb).value(pattern);
        return cb.like(
                cb.lower(cb.function("unaccent", String.class, column)),
                cb.lower(cb.function("unaccent", String.class, boundPattern)),
                LIKE_ESCAPE);
    }

    // "%", "_" e "\" escritos pelo utilizador são procurados literalmente, não como curingas do LIKE
    private static String containsPattern(String text) {
        String escaped = text.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    // Reutiliza o join com o prestador se outro filtro (ex.: bookable) já o criou: um só JOIN na consulta.
    // É um @ManyToOne, por isso o JOIN não repete linhas e o count da paginação continua certo
    @SuppressWarnings("unchecked")
    private static Join<Service, User> professional(Root<Service> root) {
        return root.getJoins().stream()
                .filter(join -> join.getAttribute().getName().equals("professional"))
                .findFirst()
                .map(join -> (Join<Service, User>) join)
                .orElseGet(() -> root.join("professional"));
    }
}
