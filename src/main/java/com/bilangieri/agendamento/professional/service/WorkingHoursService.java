package com.bilangieri.agendamento.professional.service;

import com.bilangieri.agendamento.exception.BusinessException;
import com.bilangieri.agendamento.exception.NotFoundException;
import com.bilangieri.agendamento.professional.dto.WeeklyScheduleRequest;
import com.bilangieri.agendamento.professional.dto.WorkingHoursRequest;
import com.bilangieri.agendamento.professional.dto.WorkingHoursResponse;
import com.bilangieri.agendamento.professional.entity.WorkingHours;
import com.bilangieri.agendamento.professional.repository.WorkingHoursRepository;
import com.bilangieri.agendamento.security.CurrentUserService;
import com.bilangieri.agendamento.user.entity.Role;
import com.bilangieri.agendamento.user.entity.User;
import com.bilangieri.agendamento.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkingHoursService {

    private static final Comparator<WorkingHours> BY_DAY_AND_START =
            Comparator.comparing(WorkingHours::getDayOfWeek).thenComparing(WorkingHours::getStartTime);

    private final WorkingHoursRepository workingHoursRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<WorkingHoursResponse> findByProfessional(Long professionalId) {
        User professional = userRepository.findById(professionalId)
                .filter(user -> user.getRole() == Role.PROFESSIONAL)
                .orElseThrow(() -> new NotFoundException("Prestador não encontrado com o ID: " + professionalId));

        return workingHoursRepository.findByProfessionalId(professional.getId()).stream()
                .sorted(BY_DAY_AND_START)
                .map(WorkingHoursResponse::fromEntity)
                .toList();
    }

    // O prestador autenticado substitui o seu horário semanal completo
    @Transactional
    public List<WorkingHoursResponse> replaceMine(WeeklyScheduleRequest request) {
        validate(request.hours());
        User professional = currentUserService.getUser();

        workingHoursRepository.deleteByProfessionalId(professional.getId());
        List<WorkingHours> saved = workingHoursRepository.saveAll(request.hours().stream()
                .map(hours -> WorkingHours.builder()
                        .professional(professional)
                        .dayOfWeek(hours.dayOfWeek())
                        .startTime(hours.startTime())
                        .endTime(hours.endTime())
                        .build())
                .toList());

        return saved.stream()
                .sorted(BY_DAY_AND_START)
                .map(WorkingHoursResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkingHours> blocksOn(Long professionalId, DayOfWeek dayOfWeek) {
        return workingHoursRepository.findByProfessionalIdAndDayOfWeekOrderByStartTime(professionalId, dayOfWeek);
    }

    // O agendamento tem de caber inteiramente num dos blocos de trabalho desse dia
    @Transactional(readOnly = true)
    public boolean isWithinWorkingHours(Long professionalId, LocalDateTime startAt, LocalDateTime endAt) {
        if (!startAt.toLocalDate().equals(endAt.toLocalDate())) {
            return false;
        }

        return blocksOn(professionalId, startAt.getDayOfWeek()).stream()
                .anyMatch(block -> block.contains(startAt.toLocalTime(), endAt.toLocalTime()));
    }

    private void validate(List<WorkingHoursRequest> hours) {
        for (WorkingHoursRequest block : hours) {
            if (!block.startTime().isBefore(block.endTime())) {
                throw new BusinessException("A hora de início tem de ser anterior à hora de fim ("
                        + block.dayOfWeek() + " " + block.startTime() + "-" + block.endTime() + ").");
            }
        }

        Map<DayOfWeek, List<WorkingHoursRequest>> byDay = hours.stream()
                .collect(Collectors.groupingBy(WorkingHoursRequest::dayOfWeek));

        byDay.forEach((day, blocks) -> {
            List<WorkingHoursRequest> sorted = blocks.stream()
                    .sorted(Comparator.comparing(WorkingHoursRequest::startTime))
                    .toList();
            for (int i = 1; i < sorted.size(); i++) {
                if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
                    throw new BusinessException("Os blocos de horário de " + day + " sobrepõem-se.");
                }
            }
        });
    }
}
