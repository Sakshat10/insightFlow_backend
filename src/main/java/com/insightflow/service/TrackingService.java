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
import com.insightflow.repository.ConversionGoalRepository;
import com.insightflow.entity.ConversionGoalStatus;
import com.insightflow.repository.EventRepository;
import com.insightflow.repository.PageViewRepository;
import com.insightflow.repository.ProjectRepository;
import com.insightflow.repository.SessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import com.insightflow.event.TrackingActivityCreatedEvent;

@Slf4j
@Service
public class TrackingService {

    private final ProjectRepository projectRepository;
    private final SessionRepository sessionRepository;
    private final PageViewRepository pageViewRepository;
    private final EventRepository eventRepository;
    private final ApiKeyValidationService apiKeyValidationService;
    private final ConversionGoalRepository conversionGoalRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TrackingService(ProjectRepository projectRepository,
                           SessionRepository sessionRepository,
                           PageViewRepository pageViewRepository,
                           EventRepository eventRepository,
                           ApiKeyValidationService apiKeyValidationService,
                           ConversionGoalRepository conversionGoalRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.pageViewRepository = pageViewRepository;
        this.eventRepository = eventRepository;
        this.apiKeyValidationService = apiKeyValidationService;
        this.conversionGoalRepository = conversionGoalRepository;
        this.eventPublisher = eventPublisher;
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
                .visitorId(req.getVisitorId())
                .sessionId(req.getSessionId())
                .ipAddress(ipAddress)
                .deviceType(deviceType)
                .browser(browser)
                .country("Unknown")
                .referrer(req.getReferrer())
                .entryReferrer(req.getReferrer())
                .startedAt(LocalDateTime.now())
                .isBounce(true)
                .build();
        sessionRepository.save(session);
        log.debug("Session started: {} for project {}", req.getSessionId(), project.getId());

        eventPublisher.publishEvent(new TrackingActivityCreatedEvent(
                this,
                "SESSION_START:" + session.getId(),
                com.insightflow.entity.LiveActivityType.SESSION_START,
                project.getId(),
                session.getId(),
                session.getVisitorId(),
                session.getSessionId(),
                null,
                null,
                null,
                session.getCountry(),
                session.getBrowser(),
                session.getDeviceType(),
                false,
                session.getStartedAt()
        ));

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

        Session session = sessionRepository.findBySessionId(req.getSessionId())
                .orElseThrow(() -> new BadRequestException("Session does not exist."));

        if (!session.getProjectId().equals(project.getId())) {
            throw new BadRequestException("Session does not belong to this project.");
        }

        if (req.getUrl() != null && req.getUrl().length() > 2048) {
            throw new BadRequestException("URL too long");
        }
        if (req.getTitle() != null && req.getTitle().length() > 255) {
            throw new BadRequestException("Title too long");
        }
        if (req.getReferrer() != null && req.getReferrer().length() > 255) {
            throw new BadRequestException("Referrer too long");
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

        eventPublisher.publishEvent(new TrackingActivityCreatedEvent(
                this,
                "PAGE_VIEW:" + pv.getId(),
                com.insightflow.entity.LiveActivityType.PAGE_VIEW,
                project.getId(),
                pv.getId(),
                session.getVisitorId(),
                session.getSessionId(),
                null,
                pv.getTitle(),
                pv.getUrl(),
                session.getCountry(),
                session.getBrowser(),
                session.getDeviceType(),
                false,
                pv.getCreatedAt()
        ));

        return PageViewResponse.from(pv);
    }

    @Transactional
    public EventResponse trackEvent(TrackEventRequest req, String apiKeyHeader, HttpServletRequest httpRequest) {
        Project project = resolveProject(apiKeyHeader, "track");

        Session session = sessionRepository.findBySessionId(req.getSessionId())
                .orElseThrow(() -> new BadRequestException("Session does not exist."));

        if (!session.getProjectId().equals(project.getId())) {
            throw new BadRequestException("Session does not belong to this project.");
        }

        if (req.getEventName() == null || req.getEventName().isBlank()) {
            throw new BadRequestException("Event name is required");
        }
        if (req.getEventName().trim().length() > 100) {
            throw new BadRequestException("Event name too long");
        }
        if (req.getEventCategory() != null && req.getEventCategory().length() > 100) {
            throw new BadRequestException("Event category too long");
        }
        if (req.getEventLabel() != null && req.getEventLabel().length() > 255) {
            throw new BadRequestException("Event label too long");
        }
        if (req.getEventValue() != null && req.getEventValue().length() > 255) {
            throw new BadRequestException("Event value too long");
        }
        if (req.getUrl() != null && req.getUrl().length() > 2048) {
            throw new BadRequestException("URL too long");
        }
        if (req.getProperties() != null && req.getProperties().length() > 10000) {
            throw new BadRequestException("Properties payload size too large");
        }

        String normalizedEventName = req.getEventName().trim();
        boolean isConversion = conversionGoalRepository.existsByProjectIdAndEventNameAndStatus(
                project.getId(), normalizedEventName, ConversionGoalStatus.ACTIVE);

        Event event = Event.builder()
                .sessionId(session.getId())
                .eventName(normalizedEventName)
                .eventCategory(req.getEventCategory())
                .eventLabel(req.getEventLabel())
                .eventValue(req.getEventValue())
                .url(req.getUrl())
                .properties(req.getProperties())
                .isConversion(isConversion)
                .build();

        event = eventRepository.save(event);
        log.debug("Event '{}' tracked for project {}", req.getEventName(), project.getId());

        boolean isConv = Boolean.TRUE.equals(event.getIsConversion());
        eventPublisher.publishEvent(new TrackingActivityCreatedEvent(
                this,
                (isConv ? "CONVERSION:" : "CUSTOM_EVENT:") + event.getId(),
                isConv ? com.insightflow.entity.LiveActivityType.CONVERSION : com.insightflow.entity.LiveActivityType.CUSTOM_EVENT,
                project.getId(),
                event.getId(),
                session.getVisitorId(),
                session.getSessionId(),
                event.getEventName(),
                null,
                event.getUrl(),
                session.getCountry(),
                session.getBrowser(),
                session.getDeviceType(),
                isConv,
                event.getCreatedAt()
        ));

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

    private String escapeJsString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    private String buildTrackingScript(String apiKey, String baseUrl) {
        String safeApiKey = escapeJsString(apiKey);
        String safeBaseUrl = escapeJsString(baseUrl);
        return """
                (function() {
                  if (window.InsightFlow && window.InsightFlow.__initialized) {
                    return;
                  }

                  var IF = window.InsightFlow = window.InsightFlow || {};
                  IF.__initialized = true;

                  var apiKey = '%s';
                  var baseUrl = '%s';
                  var SESSION_TIMEOUT_MS = 30 * 60 * 1000;
                  var lastTrackedUrl = '';
                  var activeSessionPromise = null;

                  function generateId(prefix) {
                    try {
                      if (typeof crypto !== 'undefined') {
                        if (typeof crypto.randomUUID === 'function') {
                          return prefix + crypto.randomUUID();
                        }
                        if (typeof crypto.getRandomValues === 'function') {
                          var buf = new Uint8Array(16);
                          crypto.getRandomValues(buf);
                          buf[6] = (buf[6] & 0x0f) | 0x40;
                          buf[8] = (buf[8] & 0x3f) | 0x80;
                          var hex = [];
                          for (var i = 0; i < 16; i++) {
                            var h = buf[i].toString(16);
                            if (h.length === 1) h = '0' + h;
                            hex.push(h);
                            if (i === 3 || i === 5 || i === 7 || i === 9) hex.push('-');
                          }
                          return prefix + hex.join('');
                        }
                      }
                    } catch (e) {}
                    var ts = Date.now().toString(36);
                    var perf = (typeof performance !== 'undefined' ? performance.now() : 0).toString(36).replace('.', '');
                    var screenInfo = (typeof window !== 'undefined' && window.screen ? (window.screen.width + 'x' + window.screen.height) : '').toString(36);
                    return prefix + ts + '-' + perf + '-' + screenInfo;
                  }

                  function getOrInitializeVisitorId() {
                    var vid = localStorage.getItem('if_vid');
                    if (!vid) {
                      vid = generateId('if_v_');
                      localStorage.setItem('if_vid', vid);
                    }
                    return vid;
                  }

                  function safeSend(endpoint, data) {
                    return new Promise(function(resolve) {
                      try {
                        var payload;
                        try {
                          var cache = [];
                          var serialized = JSON.stringify(data, function(key, val) {
                            if (typeof val === 'object' && val !== null) {
                              if (cache.indexOf(val) !== -1) {
                                return '[Circular]';
                              }
                              cache.push(val);
                            }
                            return val;
                          });
                          payload = JSON.parse(serialized);
                          cache = null;
                        } catch (e) {
                          payload = { error: 'Serialization failed' };
                        }

                        fetch(baseUrl + endpoint, {
                          method: 'POST',
                          body: JSON.stringify(payload),
                          headers: {
                            'Content-Type': 'application/json',
                            'X-API-Key': apiKey
                          },
                          keepalive: true
                        }).then(function(res) {
                          resolve(res.ok);
                        }).catch(function() {
                          resolve(false);
                        });
                      } catch (e) {
                        resolve(false);
                      }
                    });
                  }

                  function endSession() {
                    var sid = localStorage.getItem('if_sid');
                    var start = localStorage.getItem('if_session_started_at');
                    if (sid && start) {
                      var duration = Math.round((Date.now() - parseInt(start, 10)) / 1000);
                      safeSend('/tracking/session-end', { sessionId: sid, duration: duration });
                    }
                  }

                  function ensureActiveSession() {
                    if (activeSessionPromise) {
                      return activeSessionPromise;
                    }

                    activeSessionPromise = new Promise(function(resolve) {
                      var now = Date.now();
                      var vid = getOrInitializeVisitorId();
                      var sid = localStorage.getItem('if_sid');
                      var lastAct = localStorage.getItem('if_session_last_activity');
                      var needsNew = !sid || !lastAct || (now - parseInt(lastAct, 10) >= SESSION_TIMEOUT_MS);

                      if (needsNew) {
                        endSession();
                        var newSid = generateId('if_s_');
                        localStorage.setItem('if_sid', newSid);
                        localStorage.setItem('if_session_started_at', now.toString());
                        localStorage.setItem('if_session_last_activity', now.toString());

                        safeSend('/tracking/session-start', {
                          sessionId: newSid,
                          visitorId: vid,
                          referrer: document.referrer,
                          userAgent: navigator.userAgent
                        }).then(function() {
                          resolve(newSid);
                        });
                      } else {
                        localStorage.setItem('if_session_last_activity', now.toString());
                        resolve(sid);
                      }
                    }).then(function(sid) {
                      activeSessionPromise = null;
                      return sid;
                    });

                    return activeSessionPromise;
                  }

                  var lastThrottleTime = 0;
                  function updateActivity() {
                    var now = Date.now();
                    if (now - lastThrottleTime >= 15000) {
                      lastThrottleTime = now;
                      localStorage.setItem('if_session_last_activity', now.toString());
                    }
                  }

                  IF.track = function(eventName, categoryOrProps, label, value, properties) {
                    try {
                      if (typeof eventName !== 'string' || !eventName.trim()) {
                        return;
                      }
                      var name = eventName.trim();
                      var payload = { eventName: name, url: window.location.href };

                      if (categoryOrProps && typeof categoryOrProps === 'object') {
                        payload.properties = JSON.stringify(categoryOrProps);
                      } else {
                        if (categoryOrProps) payload.eventCategory = String(categoryOrProps);
                        if (label) payload.eventLabel = String(label);
                        if (value) payload.eventValue = String(value);
                        if (properties) payload.properties = JSON.stringify(properties);
                      }

                      ensureActiveSession().then(function(sid) {
                        payload.sessionId = sid;
                        safeSend('/tracking/event', payload);
                        updateActivity();
                      });
                    } catch (e) {}
                  };

                  function trackPageViewInternal() {
                    var currentUrl = window.location.href;
                    if (currentUrl === lastTrackedUrl) {
                      return;
                    }
                    lastTrackedUrl = currentUrl;

                    ensureActiveSession().then(function(sid) {
                      safeSend('/tracking/page-view', {
                        sessionId: sid,
                        url: currentUrl,
                        title: document.title,
                        referrer: document.referrer
                      });
                      updateActivity();
                    });
                  }

                  function wrapHistory(type) {
                    var orig = history[type];
                    if (typeof orig === 'function') {
                      history[type] = function() {
                        var rv = orig.apply(this, arguments);
                        setTimeout(trackPageViewInternal, 0);
                        return rv;
                      };
                    }
                  }
                  wrapHistory('pushState');
                  wrapHistory('replaceState');
                  window.addEventListener('popstate', function() {
                    setTimeout(trackPageViewInternal, 0);
                  });

                  window.addEventListener('pagehide', function() {
                    endSession();
                  });

                  var activityEvents = ['click', 'keydown', 'scroll', 'touchstart', 'visibilitychange'];
                  activityEvents.forEach(function(e) {
                    window.addEventListener(e, updateActivity, { passive: true });
                  });

                  IF.getVisitorId = function() { return localStorage.getItem('if_vid'); };
                  IF.getSessionId = function() { return localStorage.getItem('if_sid'); };

                  trackPageViewInternal();
                })();
                """.formatted(safeApiKey, safeBaseUrl);
    }
}
