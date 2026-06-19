package com.insightflow.service;

import com.insightflow.constants.ProjectConstants;
import com.insightflow.dto.*;
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

    public TrackingService(ProjectRepository projectRepository,
                           SessionRepository sessionRepository,
                           PageViewRepository pageViewRepository,
                           EventRepository eventRepository) {
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.pageViewRepository = pageViewRepository;
        this.eventRepository = eventRepository;
    }

    public String getTrackingScript(String trackingKey, HttpServletRequest request) {
        Project project = projectRepository.findByTrackingKey(trackingKey)
        .orElseThrow(() -> new ResourceNotFoundException("Project", "trackingKey", trackingKey));

if (!project.getProjectStatus().equals(ProjectConstants.ACTIVE)) {
    throw new ResourceNotFoundException("Project", "trackingKey", trackingKey);
}

        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                ? ":" + request.getServerPort() : "");

        return buildTrackingScript(trackingKey, baseUrl);
    }

    @Transactional
    public TrackSessionStartResponse trackSessionStart(TrackSessionStartRequest req,
                                                        HttpServletRequest httpRequest) {
        Project project = resolveProject(req.getTrackingKey());

        if (sessionRepository.findBySessionId(req.getSessionId()).isPresent()) {
            return TrackSessionStartResponse.builder()
                    .sessionId(req.getSessionId())
                    .trackingKey(req.getTrackingKey())
                    .build();
        }

        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = req.getUserAgent() != null ? req.getUserAgent()
                : httpRequest.getHeader("User-Agent");
        String deviceType = detectDeviceType(userAgent);
        String browser = detectBrowser(userAgent);

        Session session = Session.builder()
                .projectId(project.getId())
                .trackingKey(req.getTrackingKey())
                .sessionId(req.getSessionId())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceType(deviceType)
                .browser(browser)
                .referrer(req.getReferrer())
                .startedAt(LocalDateTime.now())
                .pageViewCount(0)
                .isBounce(true)
                .build();

        sessionRepository.save(session);
        log.debug("Session started: {} for project {}", req.getSessionId(), project.getId());

        return TrackSessionStartResponse.builder()
                .sessionId(req.getSessionId())
                .trackingKey(req.getTrackingKey())
                .build();
    }

    @Transactional
    public void trackSessionEnd(TrackSessionEndRequest req) {
        Session session = sessionRepository
                .findBySessionIdAndTrackingKey(req.getSessionId(), req.getTrackingKey())
                .orElse(null);

        if (session == null) {
            log.warn("Session not found for end event: {}", req.getSessionId());
            return;
        }

        session.setEndedAt(LocalDateTime.now());
        if (req.getDuration() != null) {
            session.setDuration(req.getDuration());
        }
        if (req.getPageViewCount() != null) {
            session.setPageViewCount(req.getPageViewCount());
            session.setIsBounce(req.getPageViewCount() <= 1);
        }
        sessionRepository.save(session);
    }

    @Transactional
    public PageViewResponse trackPageView(TrackPageViewRequest req, HttpServletRequest httpRequest) {
        Project project = resolveProject(req.getTrackingKey());
        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        PageView pv = PageView.builder()
                .projectId(project.getId())
                .trackingKey(req.getTrackingKey())
                .sessionId(req.getSessionId())
                .url(req.getUrl())
                .title(req.getTitle())
                .referrer(req.getReferrer())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceType(detectDeviceType(userAgent))
                .browser(detectBrowser(userAgent))
                .build();

        pv = pageViewRepository.save(pv);
        log.debug("PageView tracked: {} for project {}", req.getUrl(), project.getId());
        return PageViewResponse.from(pv);
    }

    @Transactional
    public EventResponse trackEvent(TrackEventRequest req, HttpServletRequest httpRequest) {
        Project project = resolveProject(req.getTrackingKey());
        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        Event event = Event.builder()
                .projectId(project.getId())
                .trackingKey(req.getTrackingKey())
                .sessionId(req.getSessionId())
                .eventName(req.getEventName())
                .eventCategory(req.getEventCategory())
                .eventLabel(req.getEventLabel())
                .eventValue(req.getEventValue())
                .url(req.getUrl())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceType(detectDeviceType(userAgent))
                .browser(detectBrowser(userAgent))
                .properties(req.getProperties())
                .build();

        event = eventRepository.save(event);
        log.debug("Event '{}' tracked for project {}", req.getEventName(), project.getId());
        return EventResponse.from(event);
    }

  private Project resolveProject(String trackingKey) {

    Project project = projectRepository.findByTrackingKey(trackingKey)
            .orElseThrow(() -> new BadRequestException("Invalid tracking key"));

    if (!project.getProjectStatus().equals(ProjectConstants.ACTIVE)) {
        throw new BadRequestException("Project is inactive");
    }

    return project;
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

    private String buildTrackingScript(String trackingKey, String baseUrl) {
        return """
                (function() {
                  var IF = window.InsightFlow = window.InsightFlow || {};
                  IF.trackingKey = '%s';
                  IF.baseUrl = '%s';
                  IF.sessionId = localStorage.getItem('if_sid') || generateId();
                  localStorage.setItem('if_sid', IF.sessionId);

                  function generateId() {
                    return 'if_' + Math.random().toString(36).substr(2,9) + '_' + Date.now();
                  }

                  function send(endpoint, data) {
                    var payload = Object.assign({ trackingKey: IF.trackingKey, sessionId: IF.sessionId }, data);
                    var blob = new Blob([JSON.stringify(payload)], { type: 'application/json' });
                    if (navigator.sendBeacon) {
                      navigator.sendBeacon(IF.baseUrl + endpoint, blob);
                    } else {
                      fetch(IF.baseUrl + endpoint, { method: 'POST', body: JSON.stringify(payload),
                        headers: { 'Content-Type': 'application/json' }, keepalive: true });
                    }
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
                """.formatted(trackingKey, baseUrl);
    }
}
