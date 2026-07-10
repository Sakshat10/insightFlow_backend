package com.insightflow;

import com.insightflow.dto.*;
import com.insightflow.entity.User;
import com.insightflow.entity.ApiKeyEnvironment;
import com.insightflow.entity.ApiKeyStatus;
import com.insightflow.exception.BadRequestException;
import com.insightflow.exception.ForbiddenException;
import com.insightflow.repository.UserRepository;
import com.insightflow.repository.ApiKeyRepository;
import com.insightflow.service.EventService;
import com.insightflow.service.FunnelService;
import com.insightflow.service.ApiKeyService;
import com.insightflow.service.ApiKeyValidationService;
import com.insightflow.service.TrackingService;
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
    private FunnelService funnelService;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyValidationService apiKeyValidationService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private TrackingService trackingService;

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.insightflow.repository.SessionRepository sessionRepository;

    @Autowired
    private com.insightflow.repository.EventRepository eventRepository;

    @Autowired
    private com.insightflow.service.AnalyticsService analyticsService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Long insertSession(int projectId, String visitorId) {
        String sql = "INSERT INTO sessions (project_id, visitor_id, session_id, started_at, country, browser, device_type, created_at, updated_at) VALUES (?, ?, ?, NOW(), 'India', 'Chrome', 'Desktop', NOW(), NOW())";
        String sessionIdStr = "sess_" + visitorId + "_" + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update(sql, projectId, visitorId, sessionIdStr);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void seedProjectEvents(int projectId) {
        Long sessionId = insertSession(projectId, "seed_visitor_" + projectId);
        insertEvent(sessionId, "page_view", LocalDateTime.now());
        insertEvent(sessionId, "button_click", LocalDateTime.now());
        insertEvent(sessionId, "signup", LocalDateTime.now());
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

        Long sessionId = insertSession(6, "visitor_test_get_events");
        insertEvent(sessionId, "page_view", LocalDateTime.now());

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

    @Test
    @Transactional
    void testCreateFunnelSuccess() {
        User currentUser = userRepository.findById(4).orElseThrow();
        seedProjectEvents(6);

        // page_view, button_click exist in project 6
        CreateFunnelRequest request = CreateFunnelRequest.builder()
                .projectId(6)
                .name("Test Funnel")
                .description("Desc")
                .steps(List.of(
                        FunnelStepRequest.builder().stepOrder(2).eventName("button_click").build(),
                        FunnelStepRequest.builder().stepOrder(1).eventName("page_view").build()
                ))
                .build();

        FunnelResponse response = funnelService.createFunnel(request, currentUser);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Test Funnel", response.getName());
        assertEquals(2, response.getSteps().size());
        
        // Assert sorting by stepOrder is enforced
        assertEquals(1, response.getSteps().get(0).getStepOrder());
        assertEquals("page_view", response.getSteps().get(0).getEventName());
        assertEquals(2, response.getSteps().get(1).getStepOrder());
        assertEquals("button_click", response.getSteps().get(1).getEventName());
    }

    @Test
    @Transactional
    void testCreateFunnelValidationFailures() {
        User currentUser = userRepository.findById(4).orElseThrow();
        seedProjectEvents(6);

        // 1. Minimum 2 steps
        CreateFunnelRequest req1 = CreateFunnelRequest.builder()
                .projectId(6)
                .name("Funnel")
                .steps(List.of(FunnelStepRequest.builder().stepOrder(1).eventName("page_view").build()))
                .build();
        assertThrows(BadRequestException.class, () -> funnelService.createFunnel(req1, currentUser));

        // 2. Non-existent eventName
        CreateFunnelRequest req2 = CreateFunnelRequest.builder()
                .projectId(6)
                .name("Funnel")
                .steps(List.of(
                        FunnelStepRequest.builder().stepOrder(1).eventName("page_view").build(),
                        FunnelStepRequest.builder().stepOrder(2).eventName("non_existent_event_name_123").build()
                ))
                .build();
        assertThrows(BadRequestException.class, () -> funnelService.createFunnel(req2, currentUser));

        // 3. Duplicate eventName
        CreateFunnelRequest req3 = CreateFunnelRequest.builder()
                .projectId(6)
                .name("Funnel")
                .steps(List.of(
                        FunnelStepRequest.builder().stepOrder(1).eventName("page_view").build(),
                        FunnelStepRequest.builder().stepOrder(2).eventName("page_view").build()
                ))
                .build();
        assertThrows(BadRequestException.class, () -> funnelService.createFunnel(req3, currentUser));

        // 4. Duplicate stepOrder
        CreateFunnelRequest req4 = CreateFunnelRequest.builder()
                .projectId(6)
                .name("Funnel")
                .steps(List.of(
                        FunnelStepRequest.builder().stepOrder(1).eventName("page_view").build(),
                        FunnelStepRequest.builder().stepOrder(1).eventName("button_click").build()
                ))
                .build();
        assertThrows(BadRequestException.class, () -> funnelService.createFunnel(req4, currentUser));

        // 5. Positive stepOrder
        CreateFunnelRequest req5 = CreateFunnelRequest.builder()
                .projectId(6)
                .name("Funnel")
                .steps(List.of(
                        FunnelStepRequest.builder().stepOrder(0).eventName("page_view").build(),
                        FunnelStepRequest.builder().stepOrder(1).eventName("button_click").build()
                ))
                .build();
        assertThrows(BadRequestException.class, () -> funnelService.createFunnel(req5, currentUser));
    }

    @Test
    @Transactional
    void testCreateFunnelUnauthorizedProject() {
        // User 4 does not own project 5 (owned by user 3)
        User currentUser = userRepository.findById(4).orElseThrow();
        seedProjectEvents(5);
        CreateFunnelRequest request = CreateFunnelRequest.builder()
                .projectId(5)
                .name("Test Funnel")
                .steps(List.of(
                        FunnelStepRequest.builder().stepOrder(1).eventName("page_view").build(),
                        FunnelStepRequest.builder().stepOrder(2).eventName("button_click").build()
                ))
                .build();
        assertThrows(ForbiddenException.class, () -> funnelService.createFunnel(request, currentUser));
    }

    @Test
    @Transactional
    void testGetFunnelsListAndById() {
        User currentUser = userRepository.findById(4).orElseThrow();
        seedProjectEvents(6);

        CreateFunnelRequest request = CreateFunnelRequest.builder()
                .projectId(6)
                .name("Funnel List Test")
                .steps(List.of(
                        FunnelStepRequest.builder().stepOrder(1).eventName("page_view").build(),
                        FunnelStepRequest.builder().stepOrder(2).eventName("button_click").build()
                ))
                .build();

        FunnelResponse created = funnelService.createFunnel(request, currentUser);

        // Get by ID
        FunnelResponse fetched = funnelService.getFunnelById(created.getId(), currentUser);
        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals("Funnel List Test", fetched.getName());
        assertEquals(2, fetched.getSteps().size());

        // Get list
        List<FunnelResponse> list = funnelService.getFunnelsByProject(6, currentUser);
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(f -> f.getId().equals(created.getId())));
    }

    @Test
    @Transactional
    void testUpdateFunnel() {
        User currentUser = userRepository.findById(4).orElseThrow();
        seedProjectEvents(6);

        CreateFunnelRequest createRequest = CreateFunnelRequest.builder()
                .projectId(6)
                .name("Initial Funnel")
                .steps(List.of(
                        FunnelStepRequest.builder().stepOrder(1).eventName("page_view").build(),
                        FunnelStepRequest.builder().stepOrder(2).eventName("button_click").build()
                ))
                .build();

        FunnelResponse created = funnelService.createFunnel(createRequest, currentUser);

        // Update
        UpdateFunnelRequest updateRequest = UpdateFunnelRequest.builder()
                .name("Updated Funnel")
                .description("Updated Desc")
                .steps(List.of(
                        FunnelStepRequest.builder().stepOrder(1).eventName("button_click").build(),
                        FunnelStepRequest.builder().stepOrder(2).eventName("signup").build()
                ))
                .build();

        FunnelResponse updated = funnelService.updateFunnel(created.getId(), updateRequest, currentUser);

        assertNotNull(updated);
        assertEquals(created.getId(), updated.getId());
        assertEquals("Updated Funnel", updated.getName());
        assertEquals("Updated Desc", updated.getDescription());
        assertEquals(2, updated.getSteps().size());
        assertEquals("button_click", updated.getSteps().get(0).getEventName());
        assertEquals("signup", updated.getSteps().get(1).getEventName());
    }

    @Test
    @Transactional
    void testDeleteFunnel() {
        User currentUser = userRepository.findById(4).orElseThrow();
        seedProjectEvents(6);

        CreateFunnelRequest createRequest = CreateFunnelRequest.builder()
                .projectId(6)
                .name("Initial Funnel")
                .steps(List.of(
                        FunnelStepRequest.builder().stepOrder(1).eventName("page_view").build(),
                        FunnelStepRequest.builder().stepOrder(2).eventName("button_click").build()
                ))
                .build();

        FunnelResponse created = funnelService.createFunnel(createRequest, currentUser);

        // Delete
        funnelService.deleteFunnel(created.getId(), currentUser);

        // Verify deletion
        assertThrows(Exception.class, () -> funnelService.getFunnelById(created.getId(), currentUser));
    }

    @Autowired
    private com.insightflow.service.ProjectService projectService;

    @Test
    @Transactional
    void testProjectCreationFlow() {
        User currentUser = userRepository.findById(4).orElseThrow();

        CreateProjectRequest request = new CreateProjectRequest();
        request.setProjectName("Test Project Simplify");
        request.setDomain("simplify.insightflow.com");

        ProjectResponse response = projectService.createProject(request, currentUser);

        // A. Project created successfully
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Test Project Simplify", response.getProjectName());

        // B. Project response contains no trackingKey (field removed)
        // Verified: ProjectResponse doesn't have a trackingKey field/method.

        // Verify no api_keys are created automatically for the new project
        long keyCount = apiKeyRepository.countByProjectId(response.getId());
        assertEquals(0L, keyCount);
    }

    @Test
    @Transactional
    void testCreateApiKeySuccess() {
        User currentUser = userRepository.findById(4).orElseThrow();

        CreateApiKeyRequest req = CreateApiKeyRequest.builder()
                .projectId(6)
                .name("Production Key")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("track", "identify"))
                .build();

        // C. POST /api-keys creates a key linked to project_id
        ApiKeyCreatedResponse response = apiKeyService.createApiKey(req, currentUser);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Production Key", response.getName());
        assertEquals(ApiKeyEnvironment.PRODUCTION, response.getEnvironment());
        assertEquals(ApiKeyStatus.ACTIVE, response.getStatus());

        // D. Raw API key is returned only once
        assertNotNull(response.getApiKey());
        assertTrue(response.getApiKey().startsWith("if_live_pk_"));
    }

    @Test
    @Transactional
    void testGetApiKeysByProjectFilter() {
        User currentUser = userRepository.findById(4).orElseThrow();

        // E. GET /api-keys?projectId=X returns only keys for project X
        List<ApiKeyResponse> listProj6 = apiKeyService.getApiKeysByProject(6, currentUser);
        for (ApiKeyResponse key : listProj6) {
            assertEquals(6, key.getProjectId());
        }
    }

    @Test
    @Transactional
    void testTrackingRequestAuthFlow() {
        User currentUser = userRepository.findById(4).orElseThrow();

        CreateApiKeyRequest req = CreateApiKeyRequest.builder()
                .projectId(6)
                .name("Ingestion Auth Key")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("track"))
                .build();
        ApiKeyCreatedResponse apiKeyCreated = apiKeyService.createApiKey(req, currentUser);

        TrackSessionStartRequest startReq = new TrackSessionStartRequest();
        startReq.setSessionId(UUID.randomUUID().toString());
        startReq.setVisitorId(UUID.randomUUID().toString());
        startReq.setReferrer("direct");
        startReq.setUserAgent("TestAgent");

        org.springframework.mock.web.MockHttpServletRequest mockRequest = new org.springframework.mock.web.MockHttpServletRequest();
        mockRequest.addHeader("User-Agent", "TestAgent");

        // F. Tracking request with valid ACTIVE API key and track permission succeeds
        TrackSessionStartResponse startRes = trackingService.trackSessionStart(startReq, apiKeyCreated.getApiKey(), mockRequest);
        assertNotNull(startRes);
        assertEquals(startReq.getSessionId(), startRes.getSessionId());

        // J. Tracking data is stored under the project_id derived from api_keys.project_id
        // (Session and pageviews are logged under project 6, as authenticated by the key)

        // K. request_count increments after successful tracking requests
        // L. last_used_at updates after successful tracking requests
        ApiKeyResponse retrieved = apiKeyService.getApiKeyById(apiKeyCreated.getId(), currentUser);
        assertEquals(1L, retrieved.getRequestCount());
        assertNotNull(retrieved.getLastUsedAt());
    }

    @Test
    @Transactional
    void testTrackingRequestInvalidKeyFails() {
        TrackSessionStartRequest startReq = new TrackSessionStartRequest();
        startReq.setSessionId(UUID.randomUUID().toString());
        startReq.setVisitorId(UUID.randomUUID().toString());

        // G. Tracking request with invalid key fails
        assertThrows(BadRequestException.class, () -> 
                trackingService.trackSessionStart(startReq, "if_live_pk_invalidkey12345", new org.springframework.mock.web.MockHttpServletRequest()));
    }

    @Test
    @Transactional
    void testTrackingRequestRevokedKeyFails() {
        User currentUser = userRepository.findById(4).orElseThrow();

        CreateApiKeyRequest req = CreateApiKeyRequest.builder()
                .projectId(6)
                .name("Key to Revoke")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("track"))
                .build();
        ApiKeyCreatedResponse apiKeyCreated = apiKeyService.createApiKey(req, currentUser);
        apiKeyService.revokeApiKey(apiKeyCreated.getId(), currentUser);

        TrackSessionStartRequest startReq = new TrackSessionStartRequest();
        startReq.setSessionId(UUID.randomUUID().toString());
        startReq.setVisitorId(UUID.randomUUID().toString());

        // H. Tracking request with revoked key fails
        assertThrows(BadRequestException.class, () -> 
                trackingService.trackSessionStart(startReq, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest()));
    }

    @Test
    @Transactional
    void testTrackingRequestLackingPermissionFails() {
        User currentUser = userRepository.findById(4).orElseThrow();

        CreateApiKeyRequest req = CreateApiKeyRequest.builder()
                .projectId(6)
                .name("Key No Ingest")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("identify")) // lacks "track"
                .build();
        ApiKeyCreatedResponse apiKeyCreated = apiKeyService.createApiKey(req, currentUser);

        TrackSessionStartRequest startReq = new TrackSessionStartRequest();
        startReq.setSessionId(UUID.randomUUID().toString());
        startReq.setVisitorId(UUID.randomUUID().toString());

        // I. Tracking request with key without track permission fails
        assertThrows(BadRequestException.class, () -> 
                trackingService.trackSessionStart(startReq, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest()));
    }

    @Test
    @Transactional
    void testMultipleSessionsForSameVisitor() {
        User currentUser = userRepository.findById(4).orElseThrow();
        CreateApiKeyRequest req = CreateApiKeyRequest.builder()
                .projectId(6)
                .name("Ingestion Key")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("track"))
                .build();
        ApiKeyCreatedResponse apiKeyCreated = apiKeyService.createApiKey(req, currentUser);

        String visitorId = "visitor_multi_sess_test";
        String session1 = "sess_1_test";
        String session2 = "sess_2_test";

        TrackSessionStartRequest startReq1 = new TrackSessionStartRequest();
        startReq1.setSessionId(session1);
        startReq1.setVisitorId(visitorId);

        TrackSessionStartRequest startReq2 = new TrackSessionStartRequest();
        startReq2.setSessionId(session2);
        startReq2.setVisitorId(visitorId);

        trackingService.trackSessionStart(startReq1, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest());
        trackingService.trackSessionStart(startReq2, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest());

        // Verify both sessions were saved and have the same visitor ID
        assertNotNull(sessionRepository.findBySessionId(session1).orElseThrow());
        assertNotNull(sessionRepository.findBySessionId(session2).orElseThrow());
        assertEquals(visitorId, sessionRepository.findBySessionId(session1).orElseThrow().getVisitorId());
        assertEquals(visitorId, sessionRepository.findBySessionId(session2).orElseThrow().getVisitorId());
    }

    @Test
    @Transactional
    void testMultiplePageViewsForSameActiveSession() {
        User currentUser = userRepository.findById(4).orElseThrow();
        CreateApiKeyRequest req = CreateApiKeyRequest.builder()
                .projectId(6)
                .name("Ingestion Key")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("track"))
                .build();
        ApiKeyCreatedResponse apiKeyCreated = apiKeyService.createApiKey(req, currentUser);

        String visitorId = "visitor_pageview_test";
        String sessionId = "sess_pageview_test";

        TrackSessionStartRequest startReq = new TrackSessionStartRequest();
        startReq.setSessionId(sessionId);
        startReq.setVisitorId(visitorId);

        trackingService.trackSessionStart(startReq, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest());

        TrackPageViewRequest pvReq1 = new TrackPageViewRequest();
        pvReq1.setSessionId(sessionId);
        pvReq1.setUrl("/home");
        pvReq1.setTitle("Home");

        TrackPageViewRequest pvReq2 = new TrackPageViewRequest();
        pvReq2.setSessionId(sessionId);
        pvReq2.setUrl("/pricing");
        pvReq2.setTitle("Pricing");

        assertNotNull(trackingService.trackPageView(pvReq1, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest()));
        assertNotNull(trackingService.trackPageView(pvReq2, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest()));
    }

    @Test
    @Transactional
    void testPayloadLimitsValidation() {
        User currentUser = userRepository.findById(4).orElseThrow();
        CreateApiKeyRequest req = CreateApiKeyRequest.builder()
                .projectId(6)
                .name("Ingestion Key")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("track"))
                .build();
        ApiKeyCreatedResponse apiKeyCreated = apiKeyService.createApiKey(req, currentUser);

        String visitorId = "visitor_limits_test";
        String sessionId = "sess_limits_test";

        TrackSessionStartRequest startReq = new TrackSessionStartRequest();
        startReq.setSessionId(sessionId);
        startReq.setVisitorId(visitorId);

        trackingService.trackSessionStart(startReq, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest());

        // Oversized event name (limit is 100)
        TrackEventRequest evReq = new TrackEventRequest();
        evReq.setSessionId(sessionId);
        evReq.setEventName("a".repeat(101));

        assertThrows(BadRequestException.class, () ->
                trackingService.trackEvent(evReq, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest()));

        // Oversized title (limit is 255)
        TrackPageViewRequest pvReq = new TrackPageViewRequest();
        pvReq.setSessionId(sessionId);
        pvReq.setUrl("/test");
        pvReq.setTitle("t".repeat(256));

        assertThrows(BadRequestException.class, () ->
                trackingService.trackPageView(pvReq, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest()));
    }

    @Test
    @Transactional
    void testSessionIdLookupProjectScoped() {
        User currentUser = userRepository.findById(4).orElseThrow();
        CreateApiKeyRequest req = CreateApiKeyRequest.builder()
                .projectId(6)
                .name("Ingestion Key")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("track"))
                .build();
        ApiKeyCreatedResponse apiKeyCreated = apiKeyService.createApiKey(req, currentUser);

        // Create a session for project 8
        Long otherSession = insertSession(8, "visitor_other_proj");
        String otherSessionId = sessionRepository.findById(otherSession).orElseThrow().getSessionId();

        // Attempting to track page view using key for project 6 but otherSessionId (project 8) must fail
        TrackPageViewRequest pvReq = new TrackPageViewRequest();
        pvReq.setSessionId(otherSessionId);
        pvReq.setUrl("/hacked");

        assertThrows(BadRequestException.class, () ->
                trackingService.trackPageView(pvReq, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest()));
    }

    private Long insertSessionWithTime(int projectId, String visitorId, LocalDateTime startedAt) {
        String sql = "INSERT INTO sessions (project_id, visitor_id, session_id, started_at, country, browser, device_type, created_at, updated_at) VALUES (?, ?, ?, ?, 'India', 'Chrome', 'Desktop', ?, ?)";
        String sessionIdStr = "sess_" + visitorId + "_" + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update(sql, projectId, visitorId, sessionIdStr, startedAt, startedAt, startedAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertEventWithCategory(Long sessionId, String eventName, String category, LocalDateTime createdAt) {
        String sql = "INSERT INTO events (session_id, event_name, event_category, is_conversion, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, sessionId, eventName, category, false, createdAt, createdAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    @Transactional
    void testEventAnalyticsImpactAndTrend() {
        User currentUser = userRepository.findById(4).orElseThrow();

        // 1. Current Period: 2026-07-01 to 2026-07-03
        LocalDate currentFrom = LocalDate.of(2026, 7, 1);
        LocalDate currentTo = LocalDate.of(2026, 7, 3);

        Long s1 = insertSessionWithTime(6, "visitorA", LocalDateTime.of(2026, 7, 1, 10, 0));
        Long s2 = insertSessionWithTime(6, "visitorB", LocalDateTime.of(2026, 7, 2, 12, 0));
        Long s3 = insertSessionWithTime(6, "visitorC", LocalDateTime.of(2026, 7, 3, 15, 0));

        insertEventWithCategory(s1, "purchase", "Ecommerce", LocalDateTime.of(2026, 7, 1, 10, 2));
        insertEventWithCategory(s1, "purchase", "Ecommerce", LocalDateTime.of(2026, 7, 1, 10, 5));
        insertEventWithCategory(s1, "click", "UI", LocalDateTime.of(2026, 7, 1, 10, 10));

        insertEventWithCategory(s2, "purchase", "Ecommerce", LocalDateTime.of(2026, 7, 2, 12, 5));

        insertEventWithCategory(s3, "click", "UI", LocalDateTime.of(2026, 7, 3, 15, 5));

        // 2. Preceding Period: 2026-06-28 to 2026-06-30
        Long s4 = insertSessionWithTime(6, "visitorA", LocalDateTime.of(2026, 6, 29, 10, 0));
        Long s5 = insertSessionWithTime(6, "visitorD", LocalDateTime.of(2026, 6, 30, 11, 0));

        insertEventWithCategory(s4, "purchase", "Ecommerce", LocalDateTime.of(2026, 6, 29, 10, 5));
        insertEventWithCategory(s5, "purchase", "Ecommerce", LocalDateTime.of(2026, 6, 30, 11, 5));

        // Query the analytics service
        List<EventAnalyticsResponse> response = analyticsService.getEventAnalytics(
                6, currentFrom, currentTo, 10, currentUser);

        assertNotNull(response);
        assertEquals(2, response.size());

        EventAnalyticsResponse purchaseAnalytics = response.stream()
                .filter(e -> "purchase".equals(e.getEventName()))
                .findFirst().orElseThrow();

        EventAnalyticsResponse clickAnalytics = response.stream()
                .filter(e -> "click".equals(e.getEventName()))
                .findFirst().orElseThrow();

        // Purchase: currentCount = 3, precedingCount = 2 -> trend = 50.0%
        // sessions containing purchase in current = s1, s2 (2 sessions out of 3 total sessions in current) -> impact = 66.7%
        // unique users in current = visitorA, visitorB -> 2
        assertEquals(3L, purchaseAnalytics.getCount());
        assertEquals(2L, purchaseAnalytics.getUniqueUsers());
        assertEquals("Ecommerce", purchaseAnalytics.getCategory());
        assertEquals(66.7, purchaseAnalytics.getImpact());
        assertEquals(50.0, purchaseAnalytics.getTrend());
        assertNotNull(purchaseAnalytics.getLastSeen());

        // Click: currentCount = 2, precedingCount = 0 -> trend = 100.0%
        // sessions containing click in current = s1, s3 (2 sessions out of 3 total sessions in current) -> impact = 66.7%
        // unique users in current = visitorA, visitorC -> 2
        assertEquals(2L, clickAnalytics.getCount());
        assertEquals(2L, clickAnalytics.getUniqueUsers());
        assertEquals("UI", clickAnalytics.getCategory());
        assertEquals(66.7, clickAnalytics.getImpact());
        assertEquals(100.0, clickAnalytics.getTrend());
        assertNotNull(clickAnalytics.getLastSeen());
    }

    @Autowired
    private com.insightflow.repository.ProjectRepository projectRepository;

    @Test
    @Transactional
    void testProjectSettingsGetAndUpdate() {
        User currentUser = userRepository.findById(4).orElseThrow();

        // 1. Get settings
        ProjectSettingsResponse settings = projectService.getProjectSettings(6, currentUser);
        assertNotNull(settings);
        assertEquals(6, settings.getProjectId());
        assertEquals("UTC", settings.getTimezone()); // default
        assertTrue(settings.getPageviewTracking()); // default
        assertFalse(settings.getSessionRecording()); // default

        // 2. Update settings
        ProjectSettingsRequest updateReq = new ProjectSettingsRequest();
        updateReq.setProjectName("Updated Portfolio");
        updateReq.setDomain("updatedportfolio.com");
        updateReq.setIndustry("SaaS");
        updateReq.setTimezone("Asia/Kolkata");
        updateReq.setPageviewTracking(false);
        updateReq.setSessionRecording(true);
        updateReq.setIpAnonymization(false);
        updateReq.setBotFiltering(false);
        updateReq.setCrossDomainTracking(true);

        ProjectSettingsResponse updated = projectService.updateProjectSettings(6, updateReq, currentUser);
        assertNotNull(updated);
        assertEquals("Updated Portfolio", updated.getProjectName());
        assertEquals("updatedportfolio.com", updated.getDomain());
        assertEquals("SaaS", updated.getIndustry());
        assertEquals("Asia/Kolkata", updated.getTimezone());
        assertFalse(updated.getPageviewTracking());
        assertTrue(updated.getSessionRecording());
        assertFalse(updated.getIpAnonymization());
        assertFalse(updated.getBotFiltering());
        assertTrue(updated.getCrossDomainTracking());

        // 3. Verify ForbiddenException for other user (User 3 owns project 5, user 4 tries to access/update it)
        assertThrows(ForbiddenException.class, () -> projectService.getProjectSettings(5, currentUser));
    }

    @Test
    @Transactional
    void testProjectHardDelete() {
        User currentUser = userRepository.findById(4).orElseThrow();

        // Create a new project
        CreateProjectRequest createReq = new CreateProjectRequest();
        createReq.setProjectName("Delete Target Project");
        createReq.setDomain("deleteme.com");
        ProjectResponse created = projectService.createProject(createReq, currentUser);
        Integer pId = created.getId();

        // Insert API key
        CreateApiKeyRequest keyReq = CreateApiKeyRequest.builder()
                .projectId(pId)
                .name("Delete Target Key")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("track"))
                .build();
        apiKeyService.createApiKey(keyReq, currentUser);

        // Insert session
        Long sessionId = insertSession(pId, "delete_visitor");
        // Insert event
        insertEvent(sessionId, "delete_event", LocalDateTime.now());

        // Verify they exist
        assertEquals(1, apiKeyRepository.countByProjectId(pId));
        assertEquals(1, sessionRepository.countByProjectId(pId));
        assertEquals(1, eventRepository.countByProjectId(pId));

        // Delete the project
        projectService.deleteProject(pId, currentUser);

        // Verify all associated data has been hard-deleted
        assertEquals(0, apiKeyRepository.countByProjectId(pId));
        assertEquals(0, sessionRepository.countByProjectId(pId));
        assertEquals(0, eventRepository.countByProjectId(pId));
        assertFalse(projectRepository.existsById(pId));
    }

    private Long insertSessionWithReferrerAndTime(int projectId, String visitorId, String entryReferrer, LocalDateTime startedAt) {
        String sql = "INSERT INTO sessions (project_id, visitor_id, session_id, started_at, entry_referrer, country, browser, device_type, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 'India', 'Chrome', 'Desktop', ?, ?)";
        String sessionIdStr = "sess_" + visitorId + "_" + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update(sql, projectId, visitorId, sessionIdStr, startedAt, entryReferrer, startedAt, startedAt);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    @Transactional
    void testTrafficSourcesAnalytics() {
        User currentUser = userRepository.findById(4).orElseThrow();

        jdbcTemplate.update("UPDATE projects SET domain = 'simplify.insightflow.com' WHERE id = 6");

        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 3);
        LocalDateTime baseTime = LocalDateTime.of(2026, 7, 2, 12, 0);

        // 1. null / empty referrer -> Direct
        insertSessionWithReferrerAndTime(6, "v1", null, baseTime);
        insertSessionWithReferrerAndTime(6, "v2", "", baseTime);

        // 2. same project domain -> Direct
        insertSessionWithReferrerAndTime(6, "v3", "https://simplify.insightflow.com/dashboard", baseTime);
        insertSessionWithReferrerAndTime(6, "v4", "https://sub.simplify.insightflow.com/pricing", baseTime);

        // 3. Google -> ORGANIC_SEARCH
        insertSessionWithReferrerAndTime(6, "v5", "https://google.com/search", baseTime);
        insertSessionWithReferrerAndTime(6, "v6", "https://google.co.uk/", baseTime);

        // 4. LinkedIn -> SOCIAL
        insertSessionWithReferrerAndTime(6, "v7", "https://linkedin.com/feed", baseTime);

        // 5. t.co / X -> SOCIAL
        insertSessionWithReferrerAndTime(6, "v8", "https://t.co/xyz", baseTime);

        // 6. Unknown external domain -> REFERRAL
        insertSessionWithReferrerAndTime(6, "v9", "https://example-blog.com/post/1", baseTime);

        // 7. Malformed URL -> Unknown / UNKNOWN
        insertSessionWithReferrerAndTime(6, "v10", "not-a-valid-url", baseTime);

        // 8. Session outside date range (should not count)
        insertSessionWithReferrerAndTime(6, "v11", "https://google.com/search", baseTime.minusDays(5));

        // 9. Two sessions from same visitor (v5) on same source -> counts as 2 sessions, 1 unique visitor
        insertSessionWithReferrerAndTime(6, "v5", "https://google.com/search", baseTime);

        // Get analytics
        TrafficSourcesResponse response = analyticsService.getTrafficSources(6, from, to, currentUser);

        assertNotNull(response);
        assertEquals(11, response.getTotalSessions());

        List<TrafficSourceItemResponse> sources = response.getSources();
        assertNotNull(sources);

        // Direct
        TrafficSourceItemResponse direct = sources.stream()
                .filter(s -> "Direct".equals(s.getSource()))
                .findFirst().orElseThrow();
        assertEquals(TrafficSourceType.DIRECT, direct.getSourceType());
        assertEquals(4, direct.getSessions());
        assertEquals(4, direct.getUniqueVisitors());
        assertEquals(36.36, direct.getPercentage());

        // Google
        TrafficSourceItemResponse google = sources.stream()
                .filter(s -> "Google".equals(s.getSource()))
                .findFirst().orElseThrow();
        assertEquals(TrafficSourceType.ORGANIC_SEARCH, google.getSourceType());
        assertEquals(3, google.getSessions());
        assertEquals(2, google.getUniqueVisitors());
        assertEquals(27.27, google.getPercentage());

        // X
        TrafficSourceItemResponse x = sources.stream()
                .filter(s -> "X".equals(s.getSource()))
                .findFirst().orElseThrow();
        assertEquals(TrafficSourceType.SOCIAL, x.getSourceType());
        assertEquals(1, x.getSessions());

        // Referral
        TrafficSourceItemResponse referral = sources.stream()
                .filter(s -> "example-blog.com".equals(s.getSource()))
                .findFirst().orElseThrow();
        assertEquals(TrafficSourceType.REFERRAL, referral.getSourceType());
        assertEquals(1, referral.getSessions());

        // Malformed / Unknown
        TrafficSourceItemResponse unknown = sources.stream()
                .filter(s -> "Unknown".equals(s.getSource()))
                .findFirst().orElseThrow();
        assertEquals(TrafficSourceType.UNKNOWN, unknown.getSourceType());
        assertEquals(1, unknown.getSessions());
    }

    @Test
    @Transactional
    void testSessionEntryReferrerNotOverwritten() {
        User currentUser = userRepository.findById(4).orElseThrow();
        
        CreateApiKeyRequest req = CreateApiKeyRequest.builder()
                .projectId(6)
                .name("Ingestion Key Settings")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("track"))
                .build();
        ApiKeyCreatedResponse apiKeyCreated = apiKeyService.createApiKey(req, currentUser);

        String sessionId = "sess_overwrite_test";
        String visitorId = "visitor_overwrite";

        TrackSessionStartRequest startReq1 = new TrackSessionStartRequest();
        startReq1.setSessionId(sessionId);
        startReq1.setVisitorId(visitorId);
        startReq1.setReferrer("https://first-referrer.com");

        org.springframework.mock.web.MockHttpServletRequest mockRequest = new org.springframework.mock.web.MockHttpServletRequest();

        trackingService.trackSessionStart(startReq1, apiKeyCreated.getApiKey(), mockRequest);

        TrackSessionStartRequest startReq2 = new TrackSessionStartRequest();
        startReq2.setSessionId(sessionId);
        startReq2.setVisitorId(visitorId);
        startReq2.setReferrer("https://second-referrer.com");

        trackingService.trackSessionStart(startReq2, apiKeyCreated.getApiKey(), mockRequest);

        com.insightflow.entity.Session session = sessionRepository.findBySessionId(sessionId).orElseThrow();
        assertEquals("https://first-referrer.com", session.getEntryReferrer());
    }

    @Autowired
    private com.insightflow.repository.ConversionGoalRepository conversionGoalRepository;
    @Autowired
    private com.insightflow.service.ConversionGoalService conversionGoalService;
    @Autowired
    private com.insightflow.service.LiveActivityService liveActivityService;
    @Autowired
    private com.insightflow.service.LiveActivityStreamService liveActivityStreamService;

    @Test
    @Transactional
    void testConversionGoalsFullSystem() {
        User currentUser = userRepository.findById(4).orElseThrow();
        User otherUser = userRepository.findById(5).orElseThrow();

        CreateConversionGoalRequest createReq = new CreateConversionGoalRequest();
        createReq.setProjectId(6);
        createReq.setName("Purchase Goal");
        createReq.setEventName("purchase");

        ConversionGoalResponse createdGoal = conversionGoalService.createConversionGoal(createReq, currentUser);
        assertNotNull(createdGoal);
        assertEquals("purchase", createdGoal.getEventName());
        assertEquals("Purchase Goal", createdGoal.getName());
        assertEquals(com.insightflow.entity.ConversionGoalStatus.ACTIVE, createdGoal.getStatus());

        assertThrows(com.insightflow.exception.DuplicateResourceException.class, () -> {
            conversionGoalService.createConversionGoal(createReq, currentUser);
        });

        assertThrows(com.insightflow.exception.ForbiddenException.class, () -> {
            conversionGoalService.createConversionGoal(createReq, otherUser);
        });

        ConversionGoalResponse deactivatedGoal = conversionGoalService.deactivateConversionGoal(createdGoal.getId(), currentUser);
        assertEquals(com.insightflow.entity.ConversionGoalStatus.INACTIVE, deactivatedGoal.getStatus());

        CreateConversionGoalRequest reactivateReq = new CreateConversionGoalRequest();
        reactivateReq.setProjectId(6);
        reactivateReq.setName("New Purchase Name");
        reactivateReq.setEventName("purchase");

        ConversionGoalResponse reactivatedGoal = conversionGoalService.createConversionGoal(reactivateReq, currentUser);
        assertEquals(createdGoal.getId(), reactivatedGoal.getId());
        assertEquals("New Purchase Name", reactivatedGoal.getName());
        assertEquals(com.insightflow.entity.ConversionGoalStatus.ACTIVE, reactivatedGoal.getStatus());

        assertThrows(com.insightflow.exception.ForbiddenException.class, () -> {
            conversionGoalService.getConversionGoalsByProject(6, otherUser);
        });
        assertThrows(com.insightflow.exception.ForbiddenException.class, () -> {
            conversionGoalService.getConversionGoalById(createdGoal.getId(), otherUser);
        });

        CreateApiKeyRequest keyReq = CreateApiKeyRequest.builder()
                .projectId(6)
                .name("Test Ingestion Key")
                .environment(ApiKeyEnvironment.PRODUCTION)
                .permissions(List.of("track"))
                .build();
        ApiKeyCreatedResponse apiKeyCreated = apiKeyService.createApiKey(keyReq, currentUser);

        String sessionId = "sess_conv_test";
        String visitorId = "visitor_conv";

        TrackSessionStartRequest startReq = new TrackSessionStartRequest();
        startReq.setSessionId(sessionId);
        startReq.setVisitorId(visitorId);
        startReq.setReferrer(null);

        org.springframework.mock.web.MockHttpServletRequest mockRequest = new org.springframework.mock.web.MockHttpServletRequest();
        trackingService.trackSessionStart(startReq, apiKeyCreated.getApiKey(), mockRequest);

        TrackEventRequest purchaseEventReq = new TrackEventRequest();
        purchaseEventReq.setSessionId(sessionId);
        purchaseEventReq.setEventName("purchase");
        EventResponse purchaseResponse = trackingService.trackEvent(purchaseEventReq, apiKeyCreated.getApiKey(), mockRequest);
        assertTrue(purchaseResponse.getIsConversion());

        TrackEventRequest clickEventReq = new TrackEventRequest();
        clickEventReq.setSessionId(sessionId);
        clickEventReq.setEventName("click");
        EventResponse clickResponse = trackingService.trackEvent(clickEventReq, apiKeyCreated.getApiKey(), mockRequest);
        assertFalse(clickResponse.getIsConversion());

        conversionGoalService.deactivateConversionGoal(createdGoal.getId(), currentUser);

        TrackEventRequest purchaseEventReq2 = new TrackEventRequest();
        purchaseEventReq2.setSessionId(sessionId);
        purchaseEventReq2.setEventName("purchase");
        EventResponse purchaseResponse2 = trackingService.trackEvent(purchaseEventReq2, apiKeyCreated.getApiKey(), mockRequest);
        assertFalse(purchaseResponse2.getIsConversion());

        conversionGoalService.createConversionGoal(reactivateReq, currentUser);

        List<DailyConversionResponse> timeline = analyticsService.getDailyConversions(6, 5, currentUser);
        assertNotNull(timeline);
        assertEquals(5, timeline.size());
        DailyConversionResponse todayStats = timeline.get(4);
        assertEquals(LocalDate.now(), todayStats.getDate());
        assertEquals(2, todayStats.getConversions());

        assertThrows(com.insightflow.exception.ForbiddenException.class, () -> {
            analyticsService.getDailyConversions(6, 5, otherUser);
        });
    }

    @Test
    @Transactional
    void testConversionGoalsHistoricalBulkUpdate() {
        User currentUser = userRepository.findById(4).orElseThrow();

        CreateProjectRequest reqA = new CreateProjectRequest();
        reqA.setProjectName("Project Bulk A");
        reqA.setDomain("bulka.com");
        ProjectResponse projA = projectService.createProject(reqA, currentUser);

        CreateProjectRequest reqB = new CreateProjectRequest();
        reqB.setProjectName("Project Bulk B");
        reqB.setDomain("bulkb.com");
        ProjectResponse projB = projectService.createProject(reqB, currentUser);

        Long sessA = insertSession(projA.getId(), "visitorA");
        insertEvent(sessA, "hist_purchase", LocalDateTime.now());

        Long sessB = insertSession(projB.getId(), "visitorB");
        insertEvent(sessB, "hist_purchase", LocalDateTime.now());

        List<com.insightflow.entity.Event> eventsA = jdbcTemplate.query("SELECT is_conversion FROM events WHERE session_id = ?",
                (rs, rowNum) -> com.insightflow.entity.Event.builder().isConversion(rs.getBoolean("is_conversion")).build(), sessA);
        assertFalse(eventsA.get(0).getIsConversion());

        List<com.insightflow.entity.Event> eventsB = jdbcTemplate.query("SELECT is_conversion FROM events WHERE session_id = ?",
                (rs, rowNum) -> com.insightflow.entity.Event.builder().isConversion(rs.getBoolean("is_conversion")).build(), sessB);
        assertFalse(eventsB.get(0).getIsConversion());

        CreateConversionGoalRequest goalReq = new CreateConversionGoalRequest();
        goalReq.setProjectId(projA.getId());
        goalReq.setName("Purchase Goal");
        goalReq.setEventName("hist_purchase");
        ConversionGoalResponse goalA = conversionGoalService.createConversionGoal(goalReq, currentUser);

        List<com.insightflow.entity.Event> eventsAAfter = jdbcTemplate.query("SELECT is_conversion FROM events WHERE session_id = ?",
                (rs, rowNum) -> com.insightflow.entity.Event.builder().isConversion(rs.getBoolean("is_conversion")).build(), sessA);
        assertTrue(eventsAAfter.get(0).getIsConversion());

        List<com.insightflow.entity.Event> eventsBAfter = jdbcTemplate.query("SELECT is_conversion FROM events WHERE session_id = ?",
                (rs, rowNum) -> com.insightflow.entity.Event.builder().isConversion(rs.getBoolean("is_conversion")).build(), sessB);
        assertFalse(eventsBAfter.get(0).getIsConversion());

        conversionGoalService.deactivateConversionGoal(goalA.getId(), currentUser);

        List<com.insightflow.entity.Event> eventsAAfterDeact = jdbcTemplate.query("SELECT is_conversion FROM events WHERE session_id = ?",
                (rs, rowNum) -> com.insightflow.entity.Event.builder().isConversion(rs.getBoolean("is_conversion")).build(), sessA);
        assertFalse(eventsAAfterDeact.get(0).getIsConversion());

        conversionGoalService.createConversionGoal(goalReq, currentUser);

        List<com.insightflow.entity.Event> eventsAAfterReact = jdbcTemplate.query("SELECT is_conversion FROM events WHERE session_id = ?",
                (rs, rowNum) -> com.insightflow.entity.Event.builder().isConversion(rs.getBoolean("is_conversion")).build(), sessA);
        assertTrue(eventsAAfterReact.get(0).getIsConversion());

        // 5. Test Reconciliation safety method
        // Manually set event to false in DB to simulate incorrect/out-of-sync historical data
        jdbcTemplate.update("UPDATE events SET is_conversion = false WHERE session_id = ?", sessA);

        // Run reconciliation
        conversionGoalService.reconcileProjectConversions(projA.getId(), currentUser);

        // Verify it was correctly synced back to true
        List<com.insightflow.entity.Event> eventsAReconciled = jdbcTemplate.query("SELECT is_conversion FROM events WHERE session_id = ?",
                (rs, rowNum) -> com.insightflow.entity.Event.builder().isConversion(rs.getBoolean("is_conversion")).build(), sessA);
        assertTrue(eventsAReconciled.get(0).getIsConversion());
    }

    private Long insertPageView(Long sessionId, String url, String title) {
        String sql = "INSERT INTO page_views (session_id, page_url, title, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, sessionId, url, title);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long insertEventWithConversion(Long sessionId, String eventName, boolean isConversion) {
        String sql = "INSERT INTO events (session_id, event_name, is_conversion, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, sessionId, eventName, isConversion);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    @Transactional
    void testLiveActivityFullSystem() {
        User currentUser = userRepository.findById(4).orElseThrow();

        CreateProjectRequest reqA = new CreateProjectRequest();
        reqA.setProjectName("Live Project A");
        reqA.setDomain("livea.com");
        ProjectResponse projA = projectService.createProject(reqA, currentUser);

        Long sess1 = insertSession(projA.getId(), "visitor_live_1");
        Long sess2 = insertSession(projA.getId(), "visitor_live_2");

        insertPageView(sess1, "/home", "Home");
        insertPageView(sess1, "/pricing", "Pricing");
        insertPageView(sess2, "/dashboard", "Dashboard");

        insertEventWithConversion(sess1, "click_btn", false);
        insertEventWithConversion(sess1, "purchase_completed", true);
        insertEventWithConversion(sess2, "scroll_footer", false);
        insertEventWithConversion(sess2, "signup_success", true);

        List<com.insightflow.dto.LiveActivityResponse> activities = liveActivityService.getRecentActivity(projA.getId(), 20, currentUser);
        assertEquals(9, activities.size());

        for (int i = 0; i < activities.size() - 1; i++) {
            assertTrue(activities.get(i).getTimestamp().compareTo(activities.get(i + 1).getTimestamp()) >= 0);
        }

        long sessionStarts = activities.stream().filter(a -> a.getType() == com.insightflow.entity.LiveActivityType.SESSION_START).count();
        long pageViews = activities.stream().filter(a -> a.getType() == com.insightflow.entity.LiveActivityType.PAGE_VIEW).count();
        long customEvents = activities.stream().filter(a -> a.getType() == com.insightflow.entity.LiveActivityType.CUSTOM_EVENT).count();
        long conversions = activities.stream().filter(a -> a.getType() == com.insightflow.entity.LiveActivityType.CONVERSION).count();

        assertEquals(2, sessionStarts);
        assertEquals(3, pageViews);
        assertEquals(2, customEvents);
        assertEquals(2, conversions);

        List<com.insightflow.dto.LiveActivityResponse> limited = liveActivityService.getRecentActivity(projA.getId(), 3, currentUser);
        assertEquals(3, limited.size());

        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = liveActivityStreamService.subscribe(projA.getId());
        assertEquals(1, liveActivityStreamService.getSubscriberCount(projA.getId()));

        com.insightflow.dto.LiveActivityResponse sampleResponse = com.insightflow.dto.LiveActivityResponse.builder()
                .activityId("PAGE_VIEW:999")
                .type(com.insightflow.entity.LiveActivityType.PAGE_VIEW)
                .projectId(projA.getId())
                .sourceId(999L)
                .visitorId("visitor_live_1")
                .sessionId("sess_live_1")
                .timestamp(LocalDateTime.now())
                .build();

        assertDoesNotThrow(() -> liveActivityStreamService.publish(projA.getId(), sampleResponse));

        liveActivityStreamService.removeEmitter(projA.getId(), emitter, "test complete");
        assertEquals(0, liveActivityStreamService.getSubscriberCount(projA.getId()));
    }
}
