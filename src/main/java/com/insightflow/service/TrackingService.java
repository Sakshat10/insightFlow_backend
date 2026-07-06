package com.insightflow.service;

import com.insightflow.constants.ProjectConstants;
import com.insightflow.dto.*;
import com.insightflow.entity.ApiKey;
import com.insightflow.entity.Event;
import com.insightflow.entity.PageView;
import com.insightflow.entity.Project;
import com.insightflow.entity.Session;
import com.insightflow.exception.BadRequestException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.EventRepository;
import com.insightflow.repository.PageViewRepository;
import com.insightflow.repository.ProjectRepository;
import com.insightflow.repository.SessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class TrackingService {

    private final ProjectRepository projectRepository;
    private final SessionRepository sessionRepository;
    private final PageViewRepository pageViewRepository;
    private final EventRepository eventRepository;
    private final ApiKeyValidationService apiKeyValidationService;

    public TrackingService(ProjectRepository projectRepository,
                           SessionRepository sessionRepository,
                           PageViewRepository pageViewRepository,
                           EventRepository eventRepository,
                           ApiKeyValidationService apiKeyValidationService) {
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.pageViewRepository = pageViewRepository;
        this.eventRepository = eventRepository;
        this.apiKeyValidationService = apiKeyValidationService;
    }

    public String getTrackingScript(String apiKey, HttpServletRequest request) {
        ApiKey validatedKey = apiKeyValidationService.validateAndIncrement(apiKey, "track");
        Project project = projectRepository.findById(validatedKey.getProjectId())
                .orElseThrow(() -> new BadRequestException("Project does not exist."));

        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                ? ":" + request.getServerPort() : "");

        return buildTrackingScript(apiKey, baseUrl);
    }

    @Transactional
    public TrackSessionStartResponse trackSessionStart(TrackSessionStartRequest req,
                                                       String apiKeyHeader,
                                                       HttpServletRequest httpRequest) {
        Project project = resolveProject(apiKeyHeader, "track");

        if (sessionRepository.findBySessionId(req.getSessionId()).isPresent()) {
            return TrackSessionStartResponse.builder()
                    .sessionId(req.getSessionId())
                    .build();
        }

        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = req.getUserAgent() != null ? req.getUserAgent()
                : httpRequest.getHeader("User-Agent");
        String deviceType = detectDeviceType(userAgent);
        String browser = detectBrowser(userAgent);

        Session session = Session.builder()
                .projectId(project.getId())
                .sessionId(req.getSessionId())
                .ipAddress(ipAddress)
                .deviceType(deviceType)
                .browser(browser)
                .country("Unknown")
                .referrer(req.getReferrer())
                .startedAt(LocalDateTime.now())
                .isBounce(true)
                .build();
        sessionRepository.save(session);
        log.debug("Session started: {} for project {}", req.getSessionId(), project.getId());

        return TrackSessionStartResponse.builder()
                .sessionId(req.getSessionId())
                .build();
    }

    @Transactional
    public void trackSessionEnd(TrackSessionEndRequest req, String apiKeyHeader) {
        Project project = resolveProject(apiKeyHeader, "track");

        Session session = sessionRepository
                .findBySessionId(req.getSessionId())
                .orElse(null);

        if (session == null) {
            log.warn("Session not found for end event: {}", req.getSessionId());
            return;
        }

        if (!session.getProjectId().equals(project.getId())) {
            throw new BadRequestException("Session does not belong to this project.");
        }

        session.setEndedAt(LocalDateTime.now());
        if (req.getDuration() != null) {
            session.setDuration(req.getDuration());
        }
        session.setIsBounce(false);
        sessionRepository.save(session);
    }

    @Transactional
    public PageViewResponse trackPageView(TrackPageViewRequest req, String apiKeyHeader, HttpServletRequest httpRequest) {
        Project project = resolveProject(apiKeyHeader, "track");
        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        Session session = sessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new BadRequestException("Session does not exist."));

        if (!session.getProjectId().equals(project.getId())) {
            throw new BadRequestException("Session does not belong to this project.");
        }

        PageView pv = PageView.builder()
                .sessionId(session.getId())
                .url(req.getUrl())
                .title(req.getTitle())
                .referrer(req.getReferrer())
                .build();
        pv = pageViewRepository.save(pv);
        log.debug(
                "PageView tracked: {} for session {}",
                req.getUrl(),
                req.getSessionId()
        );
        return PageViewResponse.from(pv);
    }

    @Transactional
    public EventResponse trackEvent(TrackEventRequest req, String apiKeyHeader, HttpServletRequest httpRequest) {
        Session session = sessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new BadRequestException("Session does not exist."));

        Project project = resolveProject(apiKeyHeader, "track");
        if (!session.getProjectId().equals(project.getId())) {
            throw new BadRequestException("Session does not belong to this project.");
        }

        Event event = Event.builder()
                .sessionId(req.getSessionId())
                .eventName(req.getEventName())
                .eventCategory(req.getEventCategory())
                .eventLabel(req.getEventLabel())
                .eventValue(req.getEventValue())
                .url(req.getUrl())
                .properties(req.getProperties())
                .isConversion(req.getIsConversion())
                .build();

        event = eventRepository.save(event);
        log.debug("Event '{}' tracked for project {}", req.getEventName(), project.getId());
        return EventResponse.from(event);
    }

    private Project resolveProject(String apiKeyHeader, String requiredPermission) {
        if (apiKeyHeader == null || apiKeyHeader.isBlank()) {
            throw new BadRequestException("API key is required");
        }
        ApiKey apiKey = apiKeyValidationService.validateAndIncrement(apiKeyHeader, requiredPermission);
        return projectRepository.findById(apiKey.getProjectId())
                .orElseThrow(() -> new BadRequestException("Project does not exist."));
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String detectDeviceType(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "Mobile";
        if (ua.contains("tablet") || ua.contains("ipad")) return "Tablet";
        return "Desktop";
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome") && !ua.contains("chromium")) return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("opera") || ua.contains("opr/")) return "Opera";
        return "Other";
    }

    private String buildTrackingScript(String apiKey, String baseUrl) {
        return """
                (function() {
                  var IF = window.InsightFlow = window.InsightFlow || {};
                  IF.apiKey = '%s';
                  IF.baseUrl = '%s';
                  IF.sessionId = localStorage.getItem('if_sid') || generateId();
                  localStorage.setItem('if_sid', IF.sessionId);

                  function generateId() {
                    return 'if_' + Math.random().toString(36).substr(2,9) + '_' + Date.now();
                  }

                  function send(endpoint, data) {
                    var payload = Object.assign({ sessionId: IF.sessionId }, data);
                    fetch(IF.baseUrl + endpoint, { 
                      method: 'POST', 
                      body: JSON.stringify(payload),
                      headers: { 
                        'Content-Type': 'application/json',
                        'X-API-Key': IF.apiKey 
                      }, 
                      keepalive: true 
                    });
                  }

                  send('/tracking/session-start', { referrer: document.referrer, userAgent: navigator.userAgent });
                  send('/tracking/page-view', { url: window.location.href, title: document.title, referrer: document.referrer });

                  window.addEventListener('beforeunload', function() {
                    send('/tracking/session-end', { duration: Math.round((Date.now() - IF.startTime) / 1000) });
                  });

                  IF.startTime = Date.now();
                  IF.track = function(eventName, category, label, value, properties) {
                    send('/tracking/event', { eventName: eventName, eventCategory: category,
                      eventLabel: label, eventValue: value, url: window.location.href,
                      properties: properties ? JSON.stringify(properties) : null });
                  };
                })();
                """.formatted(apiKey, baseUrl);
    }
}
