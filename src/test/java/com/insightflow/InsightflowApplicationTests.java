package com.insightflow;

import com.insightflow.dto.EventResponse;
import com.insightflow.dto.FunnelAnalyticsResponse;
import com.insightflow.dto.PagedResponse;
import com.insightflow.entity.User;
import com.insightflow.exception.BadRequestException;
import com.insightflow.repository.UserRepository;
import com.insightflow.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class InsightflowApplicationTests {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.insightflow.service.AnalyticsService analyticsService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Long insertSession(int projectId, String visitorId) {
        String sql = "INSERT INTO sessions (project_id, visitor_id, started_at, country, browser, device_type, created_at, updated_at) VALUES (?, ?, NOW(), 'India', 'Chrome', 'Desktop', NOW(), NOW())";
        jdbcTemplate.update(sql, projectId, visitorId);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertEvent(Long sessionId, String eventName, LocalDateTime createdAt) {
        String sql = "INSERT INTO events (session_id, event_name, is_conversion, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, sessionId, eventName, false, createdAt, createdAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertEventWithId(Long id, Long sessionId, String eventName, LocalDateTime createdAt) {
        String sql = "INSERT INTO events (id, session_id, event_name, is_conversion, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, id, sessionId, eventName, false, createdAt, createdAt);
    }

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    void testGetEventsWithSessionContext() {
        // User with ID 4 owns Project with ID 6
        User currentUser = userRepository.findById(4)
                .orElseThrow(() -> new AssertionError("Test user with id 4 not found"));

        PagedResponse<EventResponse> response = eventService.getEvents(
                6, 0, 10, "createdAt", "desc", currentUser);

        assertNotNull(response);
        assertFalse(response.getContent().isEmpty());

        for (EventResponse event : response.getContent()) {
            assertEquals("India", event.getCountry());
            assertEquals("Chrome", event.getBrowser());
            assertEquals("Desktop", event.getDeviceType());
            assertNotNull(event.getCreatedAt());
        }
    }

    @Test
    @Transactional
    void testFunnelSuccess3Steps() {
        User currentUser = userRepository.findById(4).orElseThrow();
        Long sessionId = insertSession(6, UUID.randomUUID().toString());

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);
        LocalDateTime baseTime = testFrom.atStartOfDay().plusHours(12);

        insertEvent(sessionId, "page_view", baseTime.minusMinutes(10));
        insertEvent(sessionId, "button_click", baseTime.minusMinutes(5));
        insertEvent(sessionId, "signup", baseTime);

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "button_click", "signup"), currentUser);

        assertEquals(1, response.getTotalEnteredSessions());
        assertEquals(1, response.getTotalConvertedSessions());
        assertEquals(100.0, response.getOverallConversionRate());
        assertNull(response.getBiggestDropOffStep()); // No drop offs
    }

    @Test
    @Transactional
    void testFunnelWrongOrder() {
        User currentUser = userRepository.findById(4).orElseThrow();
        Long sessionId = insertSession(6, UUID.randomUUID().toString());

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);
        LocalDateTime baseTime = testFrom.atStartOfDay().plusHours(12);

        insertEvent(sessionId, "signup", baseTime.minusMinutes(10));
        insertEvent(sessionId, "page_view", baseTime.minusMinutes(5));
        insertEvent(sessionId, "button_click", baseTime);

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "button_click", "signup"), currentUser);

        assertEquals(1, response.getTotalEnteredSessions());
        assertEquals(0, response.getTotalConvertedSessions());
        assertEquals(0.0, response.getOverallConversionRate());
        assertEquals(2, response.getBiggestDropOffStep()); // Dropped off at button_click (step 2) before reaching signup
    }

    @Test
    @Transactional
    void testFunnelMissingMiddleStep() {
        User currentUser = userRepository.findById(4).orElseThrow();
        Long sessionId = insertSession(6, UUID.randomUUID().toString());

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);
        LocalDateTime baseTime = testFrom.atStartOfDay().plusHours(12);

        insertEvent(sessionId, "page_view", baseTime.minusMinutes(10));
        insertEvent(sessionId, "signup", baseTime);

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "button_click", "signup"), currentUser);

        assertEquals(1, response.getTotalEnteredSessions());
        assertEquals(0, response.getTotalConvertedSessions());
        assertEquals(1, response.getBiggestDropOffStep()); // Dropped off at step 1 (page_view)
    }

    @Test
    @Transactional
    void testFunnelRepeatedEventAllowsCompletion() {
        User currentUser = userRepository.findById(4).orElseThrow();
        Long sessionId = insertSession(6, UUID.randomUUID().toString());

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);
        LocalDateTime baseTime = testFrom.atStartOfDay().plusHours(12);

        insertEvent(sessionId, "page_view", baseTime.minusMinutes(20));
        insertEvent(sessionId, "signup", baseTime.minusMinutes(15));
        insertEvent(sessionId, "button_click", baseTime.minusMinutes(10));
        insertEvent(sessionId, "signup", baseTime.minusMinutes(5));

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "button_click", "signup"), currentUser);

        assertEquals(1, response.getTotalEnteredSessions());
        assertEquals(1, response.getTotalConvertedSessions());
    }

    @Test
    @Transactional
    void testFunnelSameTimestampTieBreaker() {
        User currentUser = userRepository.findById(4).orElseThrow();
        Long sessionId = insertSession(6, UUID.randomUUID().toString());

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);
        LocalDateTime baseTime = testFrom.atStartOfDay().plusHours(12);

        // Delete any events with conflicting manually defined IDs to avoid primary key constraints
        jdbcTemplate.update("DELETE FROM events WHERE id IN (99991, 99992, 99993)");

        // Insert steps with identical created_at, but increasing IDs: 99991, 99992, 99993
        insertEventWithId(99991L, sessionId, "page_view", baseTime);
        insertEventWithId(99992L, sessionId, "button_click", baseTime);
        insertEventWithId(99993L, sessionId, "signup", baseTime);

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "button_click", "signup"), currentUser);

        assertEquals(1, response.getTotalEnteredSessions());
        assertEquals(1, response.getTotalConvertedSessions());
    }

    @Test
    @Transactional
    void testFunnelMultipleSessions() {
        User currentUser = userRepository.findById(4).orElseThrow();

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);
        LocalDateTime baseTime = testFrom.atStartOfDay().plusHours(12);

        // Session 1 completes all 3 steps
        Long s1 = insertSession(6, UUID.randomUUID().toString());
        insertEvent(s1, "page_view", baseTime.minusMinutes(10));
        insertEvent(s1, "button_click", baseTime.minusMinutes(5));
        insertEvent(s1, "signup", baseTime);

        // Session 2 completes 2 steps
        Long s2 = insertSession(6, UUID.randomUUID().toString());
        insertEvent(s2, "page_view", baseTime.minusMinutes(10));
        insertEvent(s2, "button_click", baseTime.minusMinutes(5));

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "button_click", "signup"), currentUser);

        assertEquals(2, response.getTotalEnteredSessions());
        assertEquals(1, response.getTotalConvertedSessions());
        assertEquals(50.0, response.getOverallConversionRate());
        assertEquals(2, response.getBiggestDropOffStep());
    }

    @Test
    @Transactional
    void testFunnelSessionFromAnotherProject() {
        User currentUser = userRepository.findById(4).orElseThrow();

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);
        LocalDateTime baseTime = testFrom.atStartOfDay().plusHours(12);

        // Session from project 8 (also owned by user 4)
        Long otherSession = insertSession(8, UUID.randomUUID().toString());
        insertEvent(otherSession, "page_view", baseTime.minusMinutes(10));
        insertEvent(otherSession, "button_click", baseTime.minusMinutes(5));
        insertEvent(otherSession, "signup", baseTime);

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "button_click", "signup"), currentUser);

        assertEquals(0, response.getTotalEnteredSessions());
    }

    @Test
    @Transactional
    void testFunnelDateRangeFiltering() {
        User currentUser = userRepository.findById(4).orElseThrow();
        Long sessionId = insertSession(6, UUID.randomUUID().toString());

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);

        // Event outside range (too early)
        insertEvent(sessionId, "page_view", testFrom.atStartOfDay().minusHours(10));

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "button_click", "signup"), currentUser);

        assertEquals(0, response.getTotalEnteredSessions());
    }

    @Test
    @Transactional
    void testFunnelZeroMatchingSessions() {
        User currentUser = userRepository.findById(4).orElseThrow();

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "button_click", "signup"), currentUser);

        assertEquals(0, response.getTotalEnteredSessions());
        assertEquals(0, response.getTotalConvertedSessions());
        assertEquals(0.0, response.getOverallConversionRate());
        assertNull(response.getBiggestDropOffStep());
    }

    @Test
    @Transactional
    void testFunnelInvalidDateRange() {
        User currentUser = userRepository.findById(4).orElseThrow();
        assertThrows(BadRequestException.class, () -> {
            analyticsService.getFunnel(6, LocalDate.now().plusDays(5), LocalDate.now().plusDays(4), List.of("page_view", "button_click"), currentUser);
        });
    }

    @Test
    @Transactional
    void testFunnelFewerThanTwoSteps() {
        User currentUser = userRepository.findById(4).orElseThrow();
        assertThrows(BadRequestException.class, () -> {
            analyticsService.getFunnel(6, LocalDate.now().plusDays(2), LocalDate.now().plusDays(3), List.of("page_view"), currentUser);
        });
    }

    @Test
    @Transactional
    void testFunnelDuplicateStepNames() {
        User currentUser = userRepository.findById(4).orElseThrow();
        Long sessionId = insertSession(6, UUID.randomUUID().toString());

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);
        LocalDateTime baseTime = testFrom.atStartOfDay().plusHours(12);

        insertEvent(sessionId, "page_view", baseTime.minusMinutes(10));
        insertEvent(sessionId, "button_click", baseTime.minusMinutes(5));
        insertEvent(sessionId, "page_view", baseTime);

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "button_click", "page_view"), currentUser);

        assertEquals(1, response.getTotalEnteredSessions());
        assertEquals(1, response.getTotalConvertedSessions());
    }

    @Test
    @Transactional
    void testFunnelConsecutiveRepeatedSteps() {
        User currentUser = userRepository.findById(4).orElseThrow();
        Long sessionId = insertSession(6, UUID.randomUUID().toString());

        LocalDate testFrom = LocalDate.now().plusDays(2);
        LocalDate testTo = LocalDate.now().plusDays(3);
        LocalDateTime baseTime = testFrom.atStartOfDay().plusHours(12);

        insertEvent(sessionId, "page_view", baseTime.minusMinutes(10));
        insertEvent(sessionId, "page_view", baseTime);

        FunnelAnalyticsResponse response = analyticsService.getFunnel(
                6, testFrom, testTo,
                List.of("page_view", "page_view"), currentUser);

        assertEquals(1, response.getTotalEnteredSessions());
        assertEquals(1, response.getTotalConvertedSessions());
    }
}
