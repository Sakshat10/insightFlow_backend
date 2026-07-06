package com.insightflow.service;

import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.exception.ForbiddenException;
import com.insightflow.exception.BadRequestException;
import com.insightflow.repository.EventRepository;
import com.insightflow.repository.PageViewRepository;
import com.insightflow.repository.ProjectRepository;
import com.insightflow.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AnalyticsService {

    private final ProjectRepository projectRepository;
    private final PageViewRepository pageViewRepository;
    private final SessionRepository sessionRepository;
    private final EventRepository eventRepository;

    public AnalyticsService(ProjectRepository projectRepository,
                            PageViewRepository pageViewRepository,
                            SessionRepository sessionRepository,
                            EventRepository eventRepository) {
        this.projectRepository = projectRepository;
        this.pageViewRepository = pageViewRepository;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
    }

    public OverviewAnalyticsResponse getOverview(Integer projectId, User currentUser) {
        validateProjectAccess(projectId, currentUser);

        LocalDateTime today = LocalDate.now().atStartOfDay();
        long totalPageViews = pageViewRepository.countByProjectId(projectId);
        long totalSessions = sessionRepository.countByProjectId(projectId);
        long totalEvents = eventRepository.countByProjectId(projectId);
        long uniqueVisitors = sessionRepository.countDistinctIpByProjectId(projectId);
        long bouncedSessions = sessionRepository.countBouncedByProjectId(projectId);
        double bounceRate = totalSessions > 0
                ? Math.round(((double) bouncedSessions / totalSessions) * 10000.0) / 100.0
                : 0.0;
        Double avgDuration = sessionRepository.avgDurationByProjectId(projectId);
        long pageViewsToday = pageViewRepository.countByProjectIdAndCreatedAtAfter(projectId, today);
        long sessionsToday = sessionRepository.countByProjectIdAndStartedAtAfter(projectId, today);

        return OverviewAnalyticsResponse.builder()
                .totalPageViews(totalPageViews)
                .totalSessions(totalSessions)
                .totalEvents(totalEvents)
                .uniqueVisitors(uniqueVisitors)
                .bounceRate(bounceRate)
                .avgSessionDurationSeconds(avgDuration != null ? Math.round(avgDuration) : 0)
                .pageViewsToday(pageViewsToday)
                .sessionsToday(sessionsToday)
                .build();
    }

    public TrafficResponse getTraffic(Integer projectId, int days, User currentUser) {
        validateProjectAccess(projectId, currentUser);

        LocalDateTime from = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        LocalDateTime to = LocalDateTime.now();

        List<Object[]> pvData = pageViewRepository.dailyPageViewsByProjectId(projectId, from, to);
        List<Object[]> sessionData = sessionRepository.dailySessionsByProjectId(projectId, from, to);

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        List<TrafficDataPoint> dataPoints = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = LocalDate.now().minusDays(days - 1L - i);
            String dateStr = date.format(fmt);
            long pvCount = getCountForDate(pvData, dateStr);
            long sessionCount = getCountForDateFromSessions(sessionData, dateStr);
            dataPoints.add(TrafficDataPoint.builder()
                    .date(dateStr)
                    .pageViews(pvCount)
                    .sessions(sessionCount)
                    .build());
        }

        return TrafficResponse.builder()
                .dataPoints(dataPoints)
                .granularity("daily")
                .build();
    }

    public List<StatEntry> getTopPages(Integer projectId, int limit, User currentUser) {
        validateProjectAccess(projectId, currentUser);
        List<Object[]> results = pageViewRepository.topPagesByProjectId(
                projectId, PageRequest.of(0, limit));
        return toStatEntryList(results);
    }

    public List<StatEntry> getEventAnalytics(Integer projectId, int limit, User currentUser) {
        validateProjectAccess(projectId, currentUser);
        List<Object[]> results = eventRepository.countByEventNameAndProjectId(
                projectId, PageRequest.of(0, limit));
        return toStatEntryList(results);
    }

    public List<StatEntry> getDeviceStats(Integer projectId, User currentUser) {
        validateProjectAccess(projectId, currentUser);
        return toStatEntryList(sessionRepository.countByDeviceTypeAndProjectId(projectId));
    }

    public List<StatEntry> getBrowserStats(Integer projectId, User currentUser) {
        validateProjectAccess(projectId, currentUser);
        return toStatEntryList(sessionRepository.countByBrowserAndProjectId(projectId));
    }

    public List<StatEntry> getCountryStats(Integer projectId, User currentUser) {
        validateProjectAccess(projectId, currentUser);
        return toStatEntryList(sessionRepository.countByCountryAndProjectId(projectId));
    }

    public List<StatEntry> getReferrerStats(Integer projectId, User currentUser) {
        validateProjectAccess(projectId, currentUser);
        return toStatEntryList(sessionRepository.countByReferrerAndProjectId(projectId));
    }

    public List<StatEntry> getSessionAnalytics(Integer projectId, User currentUser) {
        validateProjectAccess(projectId, currentUser);
        long total = sessionRepository.countByProjectId(projectId);
        long bounced = sessionRepository.countBouncedByProjectId(projectId);
        long notBounced = total - bounced;
        return List.of(
                StatEntry.builder().label("Bounced").count(bounced)
                        .percentage(total > 0 ? Math.round((double) bounced / total * 10000.0) / 100.0 : 0).build(),
                StatEntry.builder().label("Engaged").count(notBounced)
                        .percentage(total > 0 ? Math.round((double) notBounced / total * 10000.0) / 100.0 : 0).build()
        );
    }

    public EventTimelineResponse getEventTimeline(
            Integer projectId,
            LocalDate from,
            LocalDate to,
            User currentUser) {

        com.insightflow.entity.Project project = projectRepository.findById(projectId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project",
                                "id",
                                projectId
                        ));

        if (!project.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    "You do not have permission to access this project"
            );
        }

        if (!com.insightflow.constants.ProjectConstants.ACTIVE.equals(project.getProjectStatus())) {
            throw new com.insightflow.exception.BadRequestException("Project is inactive.");
        }

        String fromStr = from.atStartOfDay().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String toStr = to.atTime(23, 59, 59).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<EventTimelineProjection> dbResults = eventRepository.getEventTimeline(projectId, fromStr, toStr);

        java.util.Map<LocalDate, List<EventTimelineItem>> eventsByDate = new java.util.HashMap<>();
        for (EventTimelineProjection proj : dbResults) {
            Object rawDate = proj.getDate();
            if (rawDate == null) {
                continue;
            }

            LocalDate date;
            if (rawDate instanceof java.sql.Date) {
                date = ((java.sql.Date) rawDate).toLocalDate();
            } else if (rawDate instanceof LocalDate) {
                date = (LocalDate) rawDate;
            } else if (rawDate instanceof java.util.Date) {
                date = ((java.util.Date) rawDate).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            } else {
                date = LocalDate.parse(rawDate.toString().substring(0, 10));
            }

            String eventName = proj.getEventName() != null ? proj.getEventName().toString() : "Unknown";

            Object rawCount = proj.getCount();
            long count = 0;
            if (rawCount instanceof Number) {
                count = ((Number) rawCount).longValue();
            } else if (rawCount != null) {
                count = Long.parseLong(rawCount.toString());
            }

            EventTimelineItem item = EventTimelineItem.builder()
                    .eventName(eventName)
                    .count(count)
                    .build();
            eventsByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(item);
        }

        List<EventTimelineDay> timeline = new ArrayList<>();
        LocalDate currentDate = from;
        while (!currentDate.isAfter(to)) {
            List<EventTimelineItem> items = eventsByDate.getOrDefault(currentDate, new ArrayList<>());
            timeline.add(EventTimelineDay.builder()
                    .date(currentDate)
                    .events(items)
                    .build());
            currentDate = currentDate.plusDays(1);
        }

        return EventTimelineResponse.builder()
                .timeline(timeline)
                .build();
    }

private void validateProjectAccess(Integer projectId, User currentUser) {

    var project = projectRepository.findById(projectId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Project",
                            "id",
                            projectId
                    ));

    if (!project.getUserId().equals(currentUser.getId())) {
        throw new ForbiddenException(
                "You do not have permission to access this project"
        );
    }
}

    private List<StatEntry> toStatEntryList(List<Object[]> rows) {
        long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
        List<StatEntry> list = new ArrayList<>();
        for (Object[] row : rows) {
            String label = row[0] != null ? row[0].toString() : "Unknown";
            long count = ((Number) row[1]).longValue();
            double pct = total > 0 ? Math.round((double) count / total * 10000.0) / 100.0 : 0;
            list.add(StatEntry.builder().label(label).count(count).percentage(pct).build());
        }
        return list;
    }

    private long getCountForDate(List<Object[]> data, String dateStr) {
        for (Object[] row : data) {
            if (row[0] != null && row[0].toString().equals(dateStr)) {
                return ((Number) row[1]).longValue();
            }
        }
        return 0;
    }

    private long getCountForDateFromSessions(List<Object[]> data, String dateStr) {
        for (Object[] row : data) {
            if (row[0] != null && row[0].toString().equals(dateStr)) {
                return ((Number) row[1]).longValue();
            }
        }
        return 0;
    }

    public FunnelAnalyticsResponse getFunnel(
            Integer projectId,
            LocalDate from,
            LocalDate to,
            List<String> steps,
            User currentUser) {

        if (projectId == null) {
            throw new BadRequestException("projectId is required");
        }
        if (from == null) {
            throw new BadRequestException("from is required");
        }
        if (to == null) {
            throw new BadRequestException("to is required");
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("from date must not be after to date");
        }
        if (steps == null || steps.isEmpty()) {
            throw new BadRequestException("steps is required");
        }

        List<String> normalizedSteps = new ArrayList<>();
        for (String step : steps) {
            if (step == null) {
                throw new BadRequestException("funnel steps cannot be blank");
            }
            String trimmed = step.trim();
            if (trimmed.isEmpty()) {
                throw new BadRequestException("funnel steps cannot be blank");
            }
            normalizedSteps.add(trimmed);
        }

        if (normalizedSteps.size() < 2) {
            throw new BadRequestException("Require at least 2 non-blank funnel steps for this MVP");
        }

        validateProjectAccess(projectId, currentUser);

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        List<FunnelEventProjection> events = eventRepository.findFunnelEvents(
                projectId, fromDateTime, toExclusive, normalizedSteps);

        java.util.Map<Long, List<FunnelEventProjection>> eventsBySession = new java.util.LinkedHashMap<>();
        for (FunnelEventProjection event : events) {
            eventsBySession.computeIfAbsent(event.getSessionId(), k -> new ArrayList<>()).add(event);
        }

        int numSteps = normalizedSteps.size();
        long[] stepCounts = new long[numSteps];

        for (List<FunnelEventProjection> sessionEvents : eventsBySession.values()) {
            int matchedStepIndex = 0;
            for (FunnelEventProjection event : sessionEvents) {
                if (matchedStepIndex < numSteps 
                        && event.getEventName().equals(normalizedSteps.get(matchedStepIndex))) {
                    matchedStepIndex++;
                }
            }
            for (int i = 0; i < matchedStepIndex; i++) {
                stepCounts[i]++;
            }
        }

        List<FunnelStepAnalyticsResponse> stepResponses = new ArrayList<>();
        long firstStepSessions = stepCounts[0];

        for (int i = 0; i < numSteps; i++) {
            long currentStepSessions = stepCounts[i];

            double conversionFromPrevious = 0.0;
            if (i == 0) {
                conversionFromPrevious = currentStepSessions > 0 ? 100.0 : 0.0;
            } else {
                long previousStepSessions = stepCounts[i - 1];
                conversionFromPrevious = previousStepSessions > 0 
                        ? ((double) currentStepSessions / previousStepSessions) * 100.0
                        : 0.0;
            }
            conversionFromPrevious = Math.round(conversionFromPrevious * 100.0) / 100.0;

            double conversionFromEntry = firstStepSessions > 0
                    ? ((double) currentStepSessions / firstStepSessions) * 100.0
                    : 0.0;
            conversionFromEntry = Math.round(conversionFromEntry * 100.0) / 100.0;

            long dropOffSessions = 0;
            double dropOffRate = 0.0;
            if (i < numSteps - 1) {
                long nextStepSessions = stepCounts[i + 1];
                dropOffSessions = currentStepSessions - nextStepSessions;
                dropOffRate = currentStepSessions > 0
                        ? ((double) dropOffSessions / currentStepSessions) * 100.0
                        : 0.0;
                dropOffRate = Math.round(dropOffRate * 100.0) / 100.0;
            }

            stepResponses.add(FunnelStepAnalyticsResponse.builder()
                    .step(i + 1)
                    .eventName(normalizedSteps.get(i))
                    .sessions(currentStepSessions)
                    .conversionFromPrevious(conversionFromPrevious)
                    .conversionFromEntry(conversionFromEntry)
                    .dropOffSessions(dropOffSessions)
                    .dropOffRate(dropOffRate)
                    .build());
        }

        long totalEnteredSessions = firstStepSessions;
        long totalConvertedSessions = stepCounts[numSteps - 1];
        double overallConversionRate = totalEnteredSessions > 0
                ? ((double) totalConvertedSessions / totalEnteredSessions) * 100.0
                : 0.0;
        overallConversionRate = Math.round(overallConversionRate * 100.0) / 100.0;

        Integer biggestDropOffStep = null;
        if (numSteps >= 2 && totalEnteredSessions > 0) {
            long maxDropOff = 0;
            for (int i = 0; i < numSteps - 1; i++) {
                long currentDropOff = stepCounts[i] - stepCounts[i + 1];
                if (currentDropOff > maxDropOff) {
                    maxDropOff = currentDropOff;
                    biggestDropOffStep = i + 1;
                }
            }
        }

        return FunnelAnalyticsResponse.builder()
                .totalEnteredSessions(totalEnteredSessions)
                .totalConvertedSessions(totalConvertedSessions)
                .overallConversionRate(overallConversionRate)
                .biggestDropOffStep(biggestDropOffStep)
                .steps(stepResponses)
                .build();
    }
}
