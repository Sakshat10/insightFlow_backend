package com.insightflow.service;

import com.insightflow.constants.ProjectConstants;
import com.insightflow.dto.CreateSessionRequest;
import com.insightflow.dto.PagedResponse;
import com.insightflow.dto.SessionResponse;
import com.insightflow.entity.Project;
import com.insightflow.entity.Session;
import com.insightflow.entity.User;
import com.insightflow.exception.BadRequestException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.ProjectRepository;
import com.insightflow.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ProjectRepository projectRepository;

    public SessionService(SessionRepository sessionRepository,
                          ProjectRepository projectRepository) {
        this.sessionRepository = sessionRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request,
                                         User currentUser) {

        Project project = projectRepository.findByTrackingKey(
                        request.getTrackingKey())
                .orElseThrow(() ->
                        new BadRequestException("Invalid tracking key"));

        if (!project.getProjectStatus().equals(ProjectConstants.ACTIVE)) {
            throw new BadRequestException("Project is inactive");
        }

        if (sessionRepository.findBySessionId(
                request.getSessionId()).isPresent()) {
            throw new BadRequestException("Session ID already exists");
        }

      Session session = Session.builder()
        .projectId(project.getId())
        .sessionId(request.getSessionId())
        .ipAddress(request.getIpAddress())
        .country(request.getCountry())
        .city(request.getCity())
        .deviceType(request.getDeviceType())
        .browser(request.getBrowser())
        .os(request.getOs())
        .referrer(request.getReferrer())
        .startedAt(LocalDateTime.now())
        .isBounce(true)
        .build();

        session = sessionRepository.save(session);

        log.info(
                "Session created: {} for project {}",
                session.getSessionId(),
                project.getId()
        );

        return SessionResponse.from(session);
    }

    public PagedResponse<SessionResponse> getSessions(Integer projectId,
                                                      int page,
                                                      int size,
                                                      String sortBy,
                                                      String sortDir,
                                                      User currentUser) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return PagedResponse.of(
                sessionRepository.findByProjectId(projectId, pageable)
                        .map(SessionResponse::from)
        );
    }

    public SessionResponse getSessionById(Long id,
                                          User currentUser) {

        Session session = sessionRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Session",
                                "id",
                                id
                        ));

        return SessionResponse.from(session);
    }
}