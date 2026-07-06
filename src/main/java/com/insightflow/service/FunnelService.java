package com.insightflow.service;

import com.insightflow.dto.*;
import com.insightflow.entity.*;
import com.insightflow.exception.BadRequestException;
import com.insightflow.exception.ForbiddenException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.EventRepository;
import com.insightflow.repository.FunnelRepository;
import com.insightflow.repository.FunnelStepRepository;
import com.insightflow.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class FunnelService {

    private final FunnelRepository funnelRepository;
    private final FunnelStepRepository funnelStepRepository;
    private final ProjectRepository projectRepository;
    private final EventRepository eventRepository;

    public FunnelService(FunnelRepository funnelRepository,
                         FunnelStepRepository funnelStepRepository,
                         ProjectRepository projectRepository,
                         EventRepository eventRepository) {
        this.funnelRepository = funnelRepository;
        this.funnelStepRepository = funnelStepRepository;
        this.projectRepository = projectRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public FunnelResponse createFunnel(CreateFunnelRequest request, User currentUser) {
        if (request.getProjectId() == null) {
            throw new BadRequestException("projectId is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("name must not be blank");
        }
        if (request.getSteps() == null || request.getSteps().isEmpty()) {
            throw new BadRequestException("steps are required");
        }

        validateProjectAccess(request.getProjectId(), currentUser);
        List<FunnelStepRequest> sortedSteps = validateAndNormalizeSteps(request.getProjectId(), request.getSteps());

        Funnel funnel = Funnel.builder()
                .projectId(request.getProjectId())
                .name(request.getName().trim())
                .description(request.getDescription())
                .createdBy(currentUser.getId())
                .build();

        // Save funnel to generate ID
        funnel = funnelRepository.save(funnel);

        List<FunnelStep> steps = new ArrayList<>();
        for (FunnelStepRequest stepReq : sortedSteps) {
            steps.add(FunnelStep.builder()
                    .funnelId(funnel.getId())
                    .stepOrder(stepReq.getStepOrder())
                    .eventName(stepReq.getEventName().trim())
                    .build());
        }

        funnel.setSteps(steps);
        funnel = funnelRepository.save(funnel);

        log.info("Funnel '{}' created with {} steps by user {}", funnel.getName(), steps.size(), currentUser.getUsername());
        return FunnelResponse.from(funnel);
    }

    public List<FunnelResponse> getFunnelsByProject(Integer projectId, User currentUser) {
        if (projectId == null) {
            throw new BadRequestException("projectId is required");
        }
        validateProjectAccess(projectId, currentUser);
        List<Funnel> funnels = funnelRepository.findByProjectIdWithSteps(projectId);
        return funnels.stream().map(FunnelResponse::from).toList();
    }

    public FunnelResponse getFunnelById(Integer id, User currentUser) {
        Funnel funnel = funnelRepository.findByIdWithSteps(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funnel", "id", id));
        validateProjectAccess(funnel.getProjectId(), currentUser);
        return FunnelResponse.from(funnel);
    }

    @Transactional
    public FunnelResponse updateFunnel(Integer id, UpdateFunnelRequest request, User currentUser) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("name must not be blank");
        }
        if (request.getSteps() == null || request.getSteps().isEmpty()) {
            throw new BadRequestException("steps are required");
        }

        Funnel funnel = funnelRepository.findByIdWithSteps(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funnel", "id", id));
        validateProjectAccess(funnel.getProjectId(), currentUser);
        List<FunnelStepRequest> sortedSteps = validateAndNormalizeSteps(funnel.getProjectId(), request.getSteps());

        funnel.setName(request.getName().trim());
        funnel.setDescription(request.getDescription());

        // Clear existing steps first
        funnel.getSteps().clear();
        funnelRepository.saveAndFlush(funnel);

        // Add new steps
        List<FunnelStep> steps = new ArrayList<>();
        for (FunnelStepRequest stepReq : sortedSteps) {
            steps.add(FunnelStep.builder()
                    .funnelId(funnel.getId())
                    .stepOrder(stepReq.getStepOrder())
                    .eventName(stepReq.getEventName().trim())
                    .build());
        }
        funnel.getSteps().addAll(steps);
        funnel = funnelRepository.save(funnel);

        log.info("Funnel '{}' (id: {}) updated by user {}", funnel.getName(), funnel.getId(), currentUser.getUsername());
        return FunnelResponse.from(funnel);
    }

    @Transactional
    public FunnelResponse deleteFunnel(Integer id, User currentUser) {
        Funnel funnel = funnelRepository.findByIdWithSteps(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funnel", "id", id));
        validateProjectAccess(funnel.getProjectId(), currentUser);

        funnelRepository.delete(funnel);

        log.info("Funnel '{}' (id: {}) deleted by user {}", funnel.getName(), id, currentUser.getUsername());
        return FunnelResponse.from(funnel);
    }

    private void validateProjectAccess(Integer projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to access this project");
        }
    }

    private List<FunnelStepRequest> validateAndNormalizeSteps(Integer projectId, List<FunnelStepRequest> steps) {
        if (steps.size() < 2) {
            throw new BadRequestException("minimum 2 steps required");
        }

        Set<Integer> orders = new HashSet<>();
        Set<String> eventNames = new HashSet<>();
        List<FunnelStepRequest> mutableSteps = new ArrayList<>(steps);

        for (FunnelStepRequest step : mutableSteps) {
            if (step.getStepOrder() == null || step.getStepOrder() <= 0) {
                throw new BadRequestException("stepOrder values must be positive");
            }
            if (step.getEventName() == null || step.getEventName().trim().isEmpty()) {
                throw new BadRequestException("eventName must not be blank");
            }

            String eventName = step.getEventName().trim();
            if (!orders.add(step.getStepOrder())) {
                throw new BadRequestException("stepOrder values must be unique");
            }
            if (!eventNames.add(eventName)) {
                throw new BadRequestException("duplicate eventName steps are not allowed for this MVP");
            }

            // Verify eventName exists among events belonging to the project
            boolean eventExists = eventRepository.existsByProjectIdAndEventName(projectId, eventName);
            if (!eventExists) {
                throw new BadRequestException("eventName '" + eventName + "' does not exist in this project");
            }
        }

        // Sort steps by stepOrder
        mutableSteps.sort(Comparator.comparingInt(FunnelStepRequest::getStepOrder));
        return mutableSteps;
    }
}
