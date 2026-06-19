package com.insightflow.service;

import com.insightflow.dto.CreateEventRequest;
import com.insightflow.dto.EventResponse;
import com.insightflow.dto.PagedResponse;
import com.insightflow.entity.Event;
import com.insightflow.entity.Project;
import com.insightflow.entity.User;
import com.insightflow.exception.BadRequestException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.EventRepository;
import com.insightflow.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
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

    public EventService(EventRepository eventRepository, ProjectRepository projectRepository) {
        this.eventRepository = eventRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request, User currentUser) {
      Project project = projectRepository.findByTrackingKey(request.getTrackingKey())
        .orElseThrow(() -> new BadRequestException("Invalid tracking key"));

        Event event = Event.builder()
                .projectId(project.getId())
                .trackingKey(request.getTrackingKey())
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
        log.info("Event '{}' recorded for project {}", event.getEventName(), project.getId());
        return EventResponse.from(event);
    }

    public PagedResponse<EventResponse> getEvents(Integer projectId, int page, int size,
                                                    String sortBy, String sortDir,
                                                    User currentUser) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return PagedResponse.of(
                eventRepository.findByProjectId(projectId, pageable)
                        .map(EventResponse::from));
    }

    public EventResponse getEventById(Long id, User currentUser) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
        return EventResponse.from(event);
    }
}
