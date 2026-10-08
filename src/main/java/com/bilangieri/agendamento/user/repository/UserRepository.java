package com.bilangieri.agendamento.user.repository;


import com.bilangieri.agendamento.service.entity.Service;
import com.bilangieri.agendamento.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


}
