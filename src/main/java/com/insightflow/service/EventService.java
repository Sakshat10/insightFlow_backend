package com.insightflow.service;

import com.insightflow.constants.ProjectConstants;
import com.insightflow.dto.CreateEventRequest;
import com.insightflow.dto.EventResponse;
import com.insightflow.dto.PagedResponse;
import com.insightflow.entity.Event;
import com.insightflow.entity.Project;
import com.insightflow.entity.Session;
import com.insightflow.entity.User;
import com.insightflow.exception.BadRequestException;
import com.insightflow.exception.ForbiddenException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.EventRepository;
import com.insightflow.repository.ProjectRepository;
import com.insightflow.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ProjectRepository projectRepository;
    private final SessionRepository sessionRepository;

    public EventService(EventRepository eventRepository,
                        ProjectRepository projectRepository,
                        SessionRepository sessionRepository) {
        this.eventRepository = eventRepository;
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request,
                                     User currentUser) {

        Project project = projectRepository.findByTrackingKey(request.getTrackingKey())
                .orElseThrow(() ->
                        new BadRequestException("Invalid tracking key."));

        if (!project.getProjectStatus().equals(ProjectConstants.ACTIVE)) {
            throw new BadRequestException("Project is inactive.");
        }

        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() ->
                        new BadRequestException("Session does not exist."));

        Event event = Event.builder()
                .sessionId(request.getSessionId())
                .eventName(request.getEventName())
                .eventCategory(request.getEventCategory())
                .eventLabel(request.getEventLabel())
                .eventValue(request.getEventValue())
                .url(request.getUrl())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .country(request.getCountry())
                .deviceType(request.getDeviceType())
                .browser(request.getBrowser())
                .properties(request.getProperties())
                .build();

        event = eventRepository.save(event);

        log.info("Event '{}' recorded for session {}",
                event.getEventName(),
                session.getSessionId());

        return EventResponse.from(event);
    }

    public PagedResponse<EventResponse> getEvents(Integer projectId,
                                                  int page,
                                                  int size,
                                                  String sortBy,
                                                  String sortDir,
                                                  User currentUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project",
                                "id",
                                projectId));

        if (!project.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    "You do not have permission to access this project");
        }

        if (!project.getProjectStatus().equals(ProjectConstants.ACTIVE)) {
            throw new BadRequestException("Project is inactive.");
        }

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return PagedResponse.of(
                new PageImpl<>(
                        eventRepository.findByProjectId(projectId, pageable)
                                .stream()
                                .map(EventResponse::from)
                                .toList(),
                        pageable,
                        eventRepository.countByProjectId(projectId)
                )
        );
    }

    public EventResponse getEventById(Long id,
                                      User currentUser) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event",
                                "id",
                                id));

        Session session = sessionRepository.findById(event.getSessionId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Session",
                                "id",
                                event.getSessionId()));

        Project project = projectRepository.findById(session.getProjectId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project",
                                "id",
                                session.getProjectId()));

        if (!project.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    "You do not have permission to access this project");
        }

        return EventResponse.from(event);
    }
}