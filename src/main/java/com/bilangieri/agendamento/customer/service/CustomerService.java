package com.bilangieri.agendamento.customer.service;

import com.bilangieri.agendamento.appointment.repository.AppointmentRepository;
import com.bilangieri.agendamento.customer.dto.CustomerCreateRequest;
import com.bilangieri.agendamento.customer.dto.CustomerResponse;
import com.bilangieri.agendamento.customer.dto.CustomerUpdateRequest;
import com.bilangieri.agendamento.customer.entity.Customer;
import com.bilangieri.agendamento.customer.repository.CustomerRepository;
import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.exception.ConflictException;
import com.bilangieri.agendamento.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public CustomerResponse create(CustomerCreateRequest request) {
        // Validação de regra de negócio: o email não pode estar duplicado
        if (customerRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException("Já existe um cliente registado com este email.");
        }

        Customer customer = Customer.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .notes(request.notes())
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return CustomerResponse.fromEntity(savedCustomer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable).map(CustomerResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com o ID: " + id));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findByUserEmail(String email) {
        Customer customer = customerRepository.findByUserEmail(email)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com o email: " + email));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com o ID: " + id));

        // Se o email foi alterado, verificar se já pertence a outro cliente
        customerRepository.findByEmail(request.email()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Este email já está a ser utilizado por outro cliente.");
            }
        });

        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setNotes(request.notes());

        Customer updatedCustomer = customerRepository.save(customer);
        return CustomerResponse.fromEntity(updatedCustomer);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com o ID: " + id));

        if (appointmentRepository.existsByCustomerId(id)) {
            throw new ConflictException("Este cliente tem agendamentos associados e não pode ser removido.");
        }

        customerRepository.delete(customer);
    }
}