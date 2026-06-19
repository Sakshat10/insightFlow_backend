package com.insightflow.service;

import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.exception.ResourceNotFoundException;
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

private void validateProjectAccess(Integer projectId, User currentUser) {

    var project = projectRepository.findById(projectId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Project",
                            "id",
                            projectId
                    ));

    if (!project.getUserId().equals(currentUser.getId())) {
        throw new ResourceNotFoundException(
                "Project",
                "id",
                projectId
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
}
