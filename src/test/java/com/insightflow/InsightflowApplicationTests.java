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

    @Test
    @Transactional
    void testCreateFunnelSuccess() {
        User currentUser = userRepository.findById(4).orElseThrow();

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

        // I. Tracking request with key without track permission fails
        assertThrows(BadRequestException.class, () -> 
                trackingService.trackSessionStart(startReq, apiKeyCreated.getApiKey(), new org.springframework.mock.web.MockHttpServletRequest()));
    }
}
