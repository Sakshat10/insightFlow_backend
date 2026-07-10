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
    private final TrafficSourceClassifier trafficSourceClassifier;

    public AnalyticsService(ProjectRepository projectRepository,
                            PageViewRepository pageViewRepository,
                            SessionRepository sessionRepository,
                            EventRepository eventRepository,
                            TrafficSourceClassifier trafficSourceClassifier) {
        this.projectRepository = projectRepository;
        this.pageViewRepository = pageViewRepository;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.trafficSourceClassifier = trafficSourceClassifier;
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

    public List<EventAnalyticsResponse> getEventAnalytics(
            Integer projectId,
            LocalDate from,
            LocalDate to,
            int limit,
            User currentUser) {
        validateProjectAccess(projectId, currentUser);

        if (to == null) {
            to = LocalDate.now();
        }
        if (from == null) {
            from = to.minusDays(29);
        }

        LocalDateTime currentStart = from.atStartOfDay();
        LocalDateTime currentEnd = to.atTime(23, 59, 59);

        List<Object[]> currentResults = eventRepository.findEventAnalyticsInPeriod(
                projectId, currentStart, currentEnd, PageRequest.of(0, limit));

        long totalSessions = sessionRepository.countByProjectIdAndStartedAtBetween(
                projectId, currentStart, currentEnd);

        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevFrom = from.minusDays(days);
        LocalDate prevTo = to.minusDays(days);
        LocalDateTime precedingStart = prevFrom.atStartOfDay();
        LocalDateTime precedingEnd = prevTo.atTime(23, 59, 59);

        List<String> eventNames = new java.util.ArrayList<>();
        for (Object[] row : currentResults) {
            if (row[0] != null) {
                eventNames.add(row[0].toString());
            }
        }

        java.util.Map<String, Long> precedingCounts = new java.util.HashMap<>();
        if (!eventNames.isEmpty()) {
            List<Object[]> precedingResults = eventRepository.findEventCountsInPeriod(
                    projectId, precedingStart, precedingEnd, eventNames);
            for (Object[] row : precedingResults) {
                if (row[0] != null) {
                    precedingCounts.put(row[0].toString(), ((Number) row[1]).longValue());
                }
            }
        }

        List<EventAnalyticsResponse> responses = new java.util.ArrayList<>();
        for (Object[] row : currentResults) {
            String eventName = row[0] != null ? row[0].toString() : "Unknown";
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long uniqueUsers = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            String category = row[3] != null ? row[3].toString() : "Engagement";
            if (category == null || category.isBlank()) {
                category = "Engagement";
            }

            LocalDateTime lastSeen = null;
            Object rawLastSeen = row[4];
            if (rawLastSeen instanceof java.sql.Timestamp) {
                lastSeen = ((java.sql.Timestamp) rawLastSeen).toLocalDateTime();
            } else if (rawLastSeen instanceof LocalDateTime) {
                lastSeen = (LocalDateTime) rawLastSeen;
            } else if (rawLastSeen != null) {
                try {
                    lastSeen = java.sql.Timestamp.valueOf(rawLastSeen.toString()).toLocalDateTime();
                } catch (Exception e) {
                    try {
                        lastSeen = LocalDateTime.parse(rawLastSeen.toString());
                    } catch (Exception ex) {
                        log.warn("Failed to parse lastSeen timestamp: {}", rawLastSeen, ex);
                    }
                }
            }

            long distinctSessions = row[5] != null ? ((Number) row[5]).longValue() : 0L;

            double impact = 0.0;
            if (totalSessions > 0) {
                double rawImpact = ((double) distinctSessions / totalSessions) * 100.0;
                impact = Math.round(rawImpact * 10.0) / 10.0;
            }

            long previousCount = precedingCounts.getOrDefault(eventName, 0L);
            double trend = 0.0;
            if (previousCount == 0) {
                if (count > 0) {
                    trend = 100.0;
                } else {
                    trend = 0.0;
                }
            } else {
                double rawTrend = ((double) (count - previousCount) / previousCount) * 100.0;
                trend = Math.round(rawTrend * 10.0) / 10.0;
            }

            responses.add(EventAnalyticsResponse.builder()
                    .eventName(eventName)
                    .count(count)
                    .uniqueUsers(uniqueUsers)
                    .category(category)
                    .impact(impact)
                    .trend(trend)
                    .lastSeen(lastSeen)
                    .build());
        }

        return responses;
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

    public TrafficSourcesResponse getTrafficSources(
            Integer projectId,
            LocalDate from,
            LocalDate to,
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

        validateProjectAccess(projectId, currentUser);

        com.insightflow.entity.Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        String projectDomain = project.getDomain();

        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(23, 59, 59);

        List<Object[]> rows = sessionRepository.findEntryReferrersAndVisitorsByProjectIdAndStartedAtBetween(
                projectId, fromDateTime, toDateTime);

        if (rows.isEmpty()) {
            return TrafficSourcesResponse.builder()
                    .totalSessions(0)
                    .sources(new ArrayList<>())
                    .build();
        }

        long totalSessions = rows.size();

        java.util.Map<String, java.util.Set<String>> sourceVisitors = new java.util.LinkedHashMap<>();
        java.util.Map<String, Long> sourceSessionCounts = new java.util.LinkedHashMap<>();
        java.util.Map<String, TrafficSourceType> sourceTypes = new java.util.LinkedHashMap<>();

        for (Object[] row : rows) {
            String entryReferrer = row[0] != null ? row[0].toString() : null;
            String visitorId = row[1] != null ? row[1].toString() : "UnknownVisitor";

            TrafficSourceClassifier.ClassificationResult result = trafficSourceClassifier.classify(entryReferrer, projectDomain);
            String source = result.getSource();
            TrafficSourceType sourceType = result.getSourceType();

            sourceTypes.put(source, sourceType);
            sourceSessionCounts.put(source, sourceSessionCounts.getOrDefault(source, 0L) + 1);
            sourceVisitors.computeIfAbsent(source, k -> new java.util.HashSet<>()).add(visitorId);
        }

        List<TrafficSourceItemResponse> sourceResponses = new ArrayList<>();
        for (String source : sourceSessionCounts.keySet()) {
            long sessions = sourceSessionCounts.get(source);
            long uniqueVisitors = sourceVisitors.get(source).size();
            TrafficSourceType sourceType = sourceTypes.get(source);

            double percentage = ((double) sessions / totalSessions) * 100.0;
            percentage = Math.round(percentage * 100.0) / 100.0;

            sourceResponses.add(TrafficSourceItemResponse.builder()
                    .source(source)
                    .sourceType(sourceType)
                    .sessions(sessions)
                    .uniqueVisitors(uniqueVisitors)
                    .percentage(percentage)
                    .build());
        }

        sourceResponses.sort((a, b) -> Long.compare(b.getSessions(), a.getSessions()));

        return TrafficSourcesResponse.builder()
                .totalSessions(totalSessions)
                .sources(sourceResponses)
                .build();
    }

    public List<DailyConversionResponse> getDailyConversions(
            Integer projectId,
            Integer days,
            User currentUser) {

        if (projectId == null) {
            throw new BadRequestException("projectId is required");
        }
        int daysParam = days != null ? days : 30;
        if (daysParam <= 0) {
            throw new BadRequestException("days must be greater than zero");
        }

        validateProjectAccess(projectId, currentUser);

        LocalDate now = LocalDate.now();
        LocalDate fromDate = now.minusDays(daysParam - 1);
        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = now.atTime(23, 59, 59);

        List<Object[]> rows = eventRepository.getConversionTimeline(projectId, fromDateTime, toDateTime);

        java.util.Map<LocalDate, Long> conversionCounts = new java.util.HashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null) continue;
            LocalDate date;
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else if (row[0] instanceof java.time.LocalDate) {
                date = (java.time.LocalDate) row[0];
            } else {
                date = LocalDate.parse(row[0].toString());
            }
            long count = ((Number) row[1]).longValue();
            conversionCounts.put(date, count);
        }

        List<DailyConversionResponse> timeline = new ArrayList<>();
        for (int i = 0; i < daysParam; i++) {
            LocalDate date = fromDate.plusDays(i);
            long count = conversionCounts.getOrDefault(date, 0L);
            timeline.add(DailyConversionResponse.builder()
                    .date(date)
                    .conversions(count)
                    .build());
        }

        return timeline;
    }
}
