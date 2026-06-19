package com.insightflow.service;

import com.insightflow.constants.AppConstants;
import com.insightflow.constants.ProjectConstants;
import com.insightflow.dto.*;
import com.insightflow.entity.Project;
import com.insightflow.entity.User;
import com.insightflow.exception.ForbiddenException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.EventRepository;
import com.insightflow.repository.PageViewRepository;
import com.insightflow.repository.ProjectRepository;
import com.insightflow.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;

@Slf4j
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SessionRepository sessionRepository;
    private final PageViewRepository pageViewRepository;
    private final EventRepository eventRepository;

    public ProjectService(ProjectRepository projectRepository,
                          SessionRepository sessionRepository,
                          PageViewRepository pageViewRepository,
                          EventRepository eventRepository) {
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.pageViewRepository = pageViewRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, User currentUser) {

        String trackingKey = generateTrackingKey();

        Project project = Project.builder()
                .userId(currentUser.getId())
                .projectName(request.getProjectName())
                .domain(request.getDomain())
                .projectStatus(ProjectConstants.ACTIVE)
                .trackingKey(trackingKey)
                .build();

        project = projectRepository.save(project);

        log.info(
                "Project created: {} by user {}",
                project.getId(),
                currentUser.getUsername()
        );

        return ProjectResponse.from(project);
    }

    public PagedResponse<ProjectResponse> getAllProjects(Integer projectStatus,
                                                         int page,
                                                         int size,
                                                         String sortBy,
                                                         String sortDir,
                                                         User currentUser) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(
                page,
                Math.min(size, AppConstants.MAX_PAGE_SIZE),
                sort
        );

        Page<Project> projectPage;

        if (projectStatus != null) {
            projectPage = projectRepository.findByUserIdAndProjectStatus(
                    currentUser.getId(),
                    projectStatus,
                    pageable
            );
        } else {
            projectPage = projectRepository.findByUserId(
                    currentUser.getId(),
                    pageable
            );
        }

        return PagedResponse.of(
                projectPage.map(ProjectResponse::from)
        );
    }

    public ProjectResponse getProjectById(Integer id, User currentUser) {

        Project project = findProjectById(id);

        assertOwnership(project, currentUser);

        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse updateProject(Integer id,
                                         UpdateProjectRequest request,
                                         User currentUser) {

        Project project = findProjectById(id);

        assertOwnership(project, currentUser);

        if (StringUtils.hasText(request.getProjectName())) {
            project.setProjectName(request.getProjectName());
        }

        if (request.getDomain() != null) {
            project.setDomain(request.getDomain());
        }

        if (request.getProjectStatus() != null) {
            project.setProjectStatus(request.getProjectStatus());
        }

        project = projectRepository.save(project);

        log.info(
                "Project {} updated by user {}",
                id,
                currentUser.getUsername()
        );

        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse deleteProject(Integer id, User currentUser) {

        Project project = findProjectById(id);

        assertOwnership(project, currentUser);

        project.setProjectStatus(ProjectConstants.INACTIVE);

        project = projectRepository.save(project);

        log.info(
                "Project {} deactivated by user {}",
                id,
                currentUser.getUsername()
        );

        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse restoreProject(Integer id, User currentUser) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project",
                                "id",
                                id
                        ));

        assertOwnership(project, currentUser);

        project.setProjectStatus(ProjectConstants.ACTIVE);

        project = projectRepository.save(project);

        log.info(
                "Project {} restored by user {}",
                id,
                currentUser.getUsername()
        );

        return ProjectResponse.from(project);
    }

    public ProjectStatsResponse getProjectStats(User currentUser) {

        long totalProjects =
                projectRepository.countByUserId(currentUser.getId());

        long activeProjects =
                projectRepository.countByUserIdAndProjectStatus(
                        currentUser.getId(),
                        ProjectConstants.ACTIVE
                );

        long inactiveProjects =
                projectRepository.countByUserIdAndProjectStatus(
                        currentUser.getId(),
                        ProjectConstants.INACTIVE
                );

        long totalPageViews =
                projectRepository.findByUserId(
                                currentUser.getId(),
                                Pageable.unpaged()
                        )
                        .stream()
                        .mapToLong(p ->
                                pageViewRepository.countByProjectId(p.getId()))
                        .sum();

        long totalSessions =
                projectRepository.findByUserId(
                                currentUser.getId(),
                                Pageable.unpaged()
                        )
                        .stream()
                        .mapToLong(p ->
                                sessionRepository.countByProjectId(p.getId()))
                        .sum();

        long totalEvents =
                projectRepository.findByUserId(
                                currentUser.getId(),
                                Pageable.unpaged()
                        )
                        .stream()
                        .mapToLong(p ->
                                eventRepository.countByProjectId(p.getId()))
                        .sum();

        return ProjectStatsResponse.builder()
                .totalProjects(totalProjects)
                .activeProjects(activeProjects)
                .inactiveProjects(inactiveProjects)
                .totalPageViews(totalPageViews)
                .totalSessions(totalSessions)
                .totalEvents(totalEvents)
                .build();
    }

    private Project findProjectById(Integer id) {

        return projectRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project",
                                "id",
                                id
                        ));
    }

    private void assertOwnership(Project project, User currentUser) {

        if (!project.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException(
                    "You do not have permission to access this project"
            );
        }
    }

    private String generateTrackingKey() {

        SecureRandom random = new SecureRandom();

        byte[] bytes = new byte[16];

        random.nextBytes(bytes);

        String key =
                AppConstants.TOKEN_PREFIX +
                        HexFormat.of().formatHex(bytes);

        while (projectRepository.existsByTrackingKey(key)) {

            random.nextBytes(bytes);

            key =
                    AppConstants.TOKEN_PREFIX +
                            HexFormat.of().formatHex(bytes);
        }

        return key;
    }
}