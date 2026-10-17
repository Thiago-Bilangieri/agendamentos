package com.bilangieri.agendamento.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "users") // Usamos "users" no plural porque "user" é uma palavra reservada no PostgreSQL
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // Prestadores registam-se como PENDING e só podem fazer login depois de aprovados por um ADMIN
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 50)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    // Última mudança de email/password ou eliminação da conta, em UTC: os tokens emitidos antes deixam de valer
    @Column(name = "credentials_changed_at")
    private LocalDateTime credentialsChangedAt;

    public Instant getCredentialsChangedInstant() {
        return credentialsChangedAt == null ? null : credentialsChangedAt.toInstant(ZoneOffset.UTC);
    }

    public void markCredentialsChanged(Instant instant) {
        this.credentialsChangedAt = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}