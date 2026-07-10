package com.insightflow.service;

import com.insightflow.dto.*;
import com.insightflow.entity.*;
import com.insightflow.exception.*;
import com.insightflow.repository.ConversionGoalRepository;
import com.insightflow.repository.ProjectRepository;
import com.insightflow.repository.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ConversionGoalService {

    private final ConversionGoalRepository conversionGoalRepository;
    private final ProjectRepository projectRepository;
    private final EventRepository eventRepository;

    public ConversionGoalService(ConversionGoalRepository conversionGoalRepository,
                                 ProjectRepository projectRepository,
                                 EventRepository eventRepository) {
        this.conversionGoalRepository = conversionGoalRepository;
        this.projectRepository = projectRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public ConversionGoalResponse createConversionGoal(CreateConversionGoalRequest request, User currentUser) {
        validateProjectAccess(request.getProjectId(), currentUser);

        String normalizedEventName = request.getEventName().trim();
        Optional<ConversionGoal> existingOpt = conversionGoalRepository.findByProjectIdAndEventName(request.getProjectId(), normalizedEventName);

        if (existingOpt.isPresent()) {
            ConversionGoal existing = existingOpt.get();
            if (existing.getStatus() == ConversionGoalStatus.ACTIVE) {
                throw new DuplicateResourceException("ConversionGoal", "eventName", normalizedEventName);
            } else {
                existing.setName(request.getName().trim());
                existing.setStatus(ConversionGoalStatus.ACTIVE);
                ConversionGoal saved = conversionGoalRepository.saveAndFlush(existing);
                log.info("Reactivated ConversionGoal ID {} for project ID {}", saved.getId(), saved.getProjectId());
                
                int affected = eventRepository.updateConversionStatusByProjectAndEventName(saved.getProjectId(), saved.getEventName(), true);
                log.info("Reactivation bulk update: affectedRows={}", affected);

                return ConversionGoalResponse.from(saved);
            }
        }

        ConversionGoal newGoal = ConversionGoal.builder()
                .projectId(request.getProjectId())
                .name(request.getName().trim())
                .eventName(normalizedEventName)
                .status(ConversionGoalStatus.ACTIVE)
                .createdBy(currentUser.getId())
                .build();

        ConversionGoal saved = conversionGoalRepository.saveAndFlush(newGoal);
        log.info("Created ConversionGoal ID {} for project ID {}", saved.getId(), saved.getProjectId());

        int affected = eventRepository.updateConversionStatusByProjectAndEventName(saved.getProjectId(), saved.getEventName(), true);
        log.info("Creation bulk update: affectedRows={}", affected);

        return ConversionGoalResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ConversionGoalResponse> getConversionGoalsByProject(Integer projectId, User currentUser) {
        validateProjectAccess(projectId, currentUser);
        List<ConversionGoal> goals = conversionGoalRepository.findByProjectId(projectId);
        return goals.stream().map(ConversionGoalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ConversionGoalResponse getConversionGoalById(Integer id, User currentUser) {
        ConversionGoal goal = conversionGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ConversionGoal", "id", id));
        validateProjectAccess(goal.getProjectId(), currentUser);
        return ConversionGoalResponse.from(goal);
    }

    @Transactional
    public ConversionGoalResponse updateConversionGoal(Integer id, UpdateConversionGoalRequest request, User currentUser) {
        ConversionGoal goal = conversionGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ConversionGoal", "id", id));
        validateProjectAccess(goal.getProjectId(), currentUser);

        ConversionGoalStatus oldStatus = goal.getStatus();
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            goal.setName(request.getName().trim());
        }
        if (request.getStatus() != null) {
            goal.setStatus(request.getStatus());
        }

        ConversionGoal saved = conversionGoalRepository.saveAndFlush(goal);
        log.info("Updated ConversionGoal ID {}", saved.getId());

        if (oldStatus == ConversionGoalStatus.ACTIVE && saved.getStatus() == ConversionGoalStatus.INACTIVE) {
            boolean otherActiveExists = conversionGoalRepository.existsByProjectIdAndEventNameAndStatus(
                    saved.getProjectId(), saved.getEventName(), ConversionGoalStatus.ACTIVE);
            if (!otherActiveExists) {
                int affected = eventRepository.updateConversionStatusByProjectAndEventName(saved.getProjectId(), saved.getEventName(), false);
                log.info("Deactivation bulk update: affectedRows={}", affected);
            }
        } else if (oldStatus == ConversionGoalStatus.INACTIVE && saved.getStatus() == ConversionGoalStatus.ACTIVE) {
            int affected = eventRepository.updateConversionStatusByProjectAndEventName(saved.getProjectId(), saved.getEventName(), true);
            log.info("Activation bulk update: affectedRows={}", affected);
        }

        return ConversionGoalResponse.from(saved);
    }

    @Transactional
    public ConversionGoalResponse deactivateConversionGoal(Integer id, User currentUser) {
        ConversionGoal goal = conversionGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ConversionGoal", "id", id));
        validateProjectAccess(goal.getProjectId(), currentUser);

        goal.setStatus(ConversionGoalStatus.INACTIVE);
        ConversionGoal saved = conversionGoalRepository.saveAndFlush(goal);
        log.info("Soft deleted/deactivated ConversionGoal ID {}", saved.getId());

        boolean otherActiveExists = conversionGoalRepository.existsByProjectIdAndEventNameAndStatus(
                saved.getProjectId(), saved.getEventName(), ConversionGoalStatus.ACTIVE);
        if (!otherActiveExists) {
            int affected = eventRepository.updateConversionStatusByProjectAndEventName(saved.getProjectId(), saved.getEventName(), false);
            log.info("Deactivation bulk update: affectedRows={}", affected);
        }

        return ConversionGoalResponse.from(saved);
    }

    private void validateProjectAccess(Integer projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to access this project");
        }
    }

    @Transactional
    public void reconcileProjectConversions(Integer projectId, User currentUser) {
        validateProjectAccess(projectId, currentUser);

        int resetAffected = eventRepository.updateConversionStatusByProject(projectId, false);
        log.info("Reconciliation: reset {} events to isConversion=false", resetAffected);

        List<ConversionGoal> activeGoals = conversionGoalRepository.findByProjectId(projectId)
                .stream()
                .filter(g -> g.getStatus() == ConversionGoalStatus.ACTIVE)
                .toList();

        for (ConversionGoal goal : activeGoals) {
            int activeAffected = eventRepository.updateConversionStatusByProjectAndEventName(
                    projectId, goal.getEventName(), true);
            log.info("Reconciliation: set {} events for eventName='{}' to isConversion=true", activeAffected, goal.getEventName());
        }
    }
}
