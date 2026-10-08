package com.bilangieri.agendamento.customer.repository;

import com.bilangieri.agendamento.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // O Spring Data JPA lê o nome deste método e cria a query automaticamente:
    // SELECT * FROM customers WHERE email = ?;
    Optional<Customer> findByEmail(String email);
}