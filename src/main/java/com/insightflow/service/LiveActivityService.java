package com.insightflow.service;

import com.insightflow.dto.LiveActivityResponse;
import com.insightflow.entity.*;
import com.insightflow.exception.ForbiddenException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LiveActivityService {

    private final SessionRepository sessionRepository;
    private final PageViewRepository pageViewRepository;
    private final EventRepository eventRepository;
    private final ProjectRepository projectRepository;

    public LiveActivityService(SessionRepository sessionRepository,
                               PageViewRepository pageViewRepository,
                               EventRepository eventRepository,
                               ProjectRepository projectRepository) {
        this.sessionRepository = sessionRepository;
        this.pageViewRepository = pageViewRepository;
        this.eventRepository = eventRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<LiveActivityResponse> getRecentActivity(Integer projectId, int limit, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (!project.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have permission to access this project");
        }

        Pageable pageable = PageRequest.of(0, limit);
        Pageable sessionPageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startedAt"));

        List<Session> recentSessions = sessionRepository.findByProjectId(projectId, sessionPageable).getContent();
        List<PageView> recentPageViews = pageViewRepository.findRecentByProjectId(projectId, pageable);
        List<Event> recentEvents = eventRepository.findRecentByProjectId(projectId, pageable);

        Set<Long> sessionIdsToLookup = new HashSet<>();
        recentPageViews.forEach(pv -> sessionIdsToLookup.add(pv.getSessionId()));
        recentEvents.forEach(e -> sessionIdsToLookup.add(e.getSessionId()));

        Map<Long, Session> sessionMap = new HashMap<>();
        recentSessions.forEach(s -> sessionMap.put(s.getId(), s));

        Set<Long> missingSessionIds = sessionIdsToLookup.stream()
                .filter(id -> !sessionMap.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingSessionIds.isEmpty()) {
            List<Session> extraSessions = sessionRepository.findAllById(missingSessionIds);
            extraSessions.forEach(s -> sessionMap.put(s.getId(), s));
        }

        List<LiveActivityResponse> merged = new ArrayList<>();

        for (Session session : recentSessions) {
            merged.add(LiveActivityResponse.builder()
                    .activityId("SESSION_START:" + session.getId())
                    .type(LiveActivityType.SESSION_START)
                    .projectId(projectId)
                    .sourceId(session.getId())
                    .visitorId(session.getVisitorId())
                    .sessionId(session.getSessionId())
                    .country(session.getCountry())
                    .browser(session.getBrowser())
                    .deviceType(session.getDeviceType())
                    .isConversion(false)
                    .timestamp(session.getStartedAt())
                    .build());
        }

        for (PageView pv : recentPageViews) {
            Session session = sessionMap.get(pv.getSessionId());
            merged.add(LiveActivityResponse.builder()
                    .activityId("PAGE_VIEW:" + pv.getId())
                    .type(LiveActivityType.PAGE_VIEW)
                    .projectId(projectId)
                    .sourceId(pv.getId())
                    .visitorId(session != null ? session.getVisitorId() : null)
                    .sessionId(session != null ? session.getSessionId() : null)
                    .title(pv.getTitle())
                    .url(pv.getUrl())
                    .country(session != null ? session.getCountry() : null)
                    .browser(session != null ? session.getBrowser() : null)
                    .deviceType(session != null ? session.getDeviceType() : null)
                    .isConversion(false)
                    .timestamp(pv.getCreatedAt())
                    .build());
        }

        for (Event event : recentEvents) {
            Session session = sessionMap.get(event.getSessionId());
            boolean isConv = Boolean.TRUE.equals(event.getIsConversion());
            merged.add(LiveActivityResponse.builder()
                    .activityId((isConv ? "CONVERSION:" : "CUSTOM_EVENT:") + event.getId())
                    .type(isConv ? LiveActivityType.CONVERSION : LiveActivityType.CUSTOM_EVENT)
                    .projectId(projectId)
                    .sourceId(event.getId())
                    .visitorId(session != null ? session.getVisitorId() : null)
                    .sessionId(session != null ? session.getSessionId() : null)
                    .eventName(event.getEventName())
                    .url(event.getUrl())
                    .country(session != null ? session.getCountry() : null)
                    .browser(session != null ? session.getBrowser() : null)
                    .deviceType(session != null ? session.getDeviceType() : null)
                    .isConversion(isConv)
                    .timestamp(event.getCreatedAt())
                    .build());
        }

        merged.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        if (merged.size() > limit) {
            return merged.subList(0, limit);
        }
        return merged;
    }
}
