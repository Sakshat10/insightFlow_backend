package com.insightflow.service;

import com.insightflow.constants.AppConstants;
import com.insightflow.constants.ProjectConstants;
import com.insightflow.dto.*;
import com.insightflow.entity.Project;
import com.insightflow.entity.User;
import com.insightflow.exception.ForbiddenException;
import com.insightflow.exception.BadRequestException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.EventRepository;
import com.insightflow.repository.PageViewRepository;
import com.insightflow.repository.ProjectRepository;
import com.insightflow.repository.SessionRepository;
import com.insightflow.repository.FunnelRepository;
import com.insightflow.repository.FunnelStepRepository;
import com.insightflow.repository.ApiKeyRepository;
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
        private final FunnelRepository funnelRepository;
        private final FunnelStepRepository funnelStepRepository;
        private final ApiKeyRepository apiKeyRepository;

        public ProjectService(ProjectRepository projectRepository,
                        SessionRepository sessionRepository,
                        PageViewRepository pageViewRepository,
                        EventRepository eventRepository,
                        FunnelRepository funnelRepository,
                        FunnelStepRepository funnelStepRepository,
                        ApiKeyRepository apiKeyRepository) {
                this.projectRepository = projectRepository;
                this.sessionRepository = sessionRepository;
                this.pageViewRepository = pageViewRepository;
                this.eventRepository = eventRepository;
                this.funnelRepository = funnelRepository;
                this.funnelStepRepository = funnelStepRepository;
                this.apiKeyRepository = apiKeyRepository;
        }

        @Transactional
        public ProjectResponse createProject(CreateProjectRequest request, User currentUser) {

                if (StringUtils.hasText(request.getDomain()) && projectRepository.existsByDomain(request.getDomain())) {
                        throw new BadRequestException("A project with this domain already exists.");
                }

                Project project = Project.builder()
                                .userId(currentUser.getId())
                                .projectName(request.getProjectName())
                                .domain(request.getDomain())
                                .projectStatus(ProjectConstants.ACTIVE)
                                .build();

                project = projectRepository.save(project);

                log.info(
                                "Project created: {} by user {}",
                                project.getId(),
                                currentUser.getUsername());

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
                                sort);

                Page<Project> projectPage;

                if (projectStatus != null) {
                        projectPage = projectRepository.findByUserIdAndProjectStatus(
                                        currentUser.getId(),
                                        projectStatus,
                                        pageable);
                } else {
                        projectPage = projectRepository.findByUserId(
                                        currentUser.getId(),
                                        pageable);
                }

                return PagedResponse.of(
                                projectPage.map(ProjectResponse::from));
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
                        if (StringUtils.hasText(request.getDomain())
                                        && !request.getDomain().equals(project.getDomain())) {
                                if (projectRepository.existsByDomain(request.getDomain())) {
                                        throw new BadRequestException("A project with this domain already exists.");
                                }
                        }
                        project.setDomain(request.getDomain());
                }

                if (request.getProjectStatus() != null) {
                        project.setProjectStatus(request.getProjectStatus());
                }

                project = projectRepository.save(project);

                log.info(
                                "Project {} updated by user {}",
                                id,
                                currentUser.getUsername());

                return ProjectResponse.from(project);
        }

        @Transactional
        public ProjectResponse deleteProject(Integer id, User currentUser) {

                Project project = projectRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Project",
                                                "id",
                                                id));

                assertOwnership(project, currentUser);

                eventRepository.deleteByProjectId(id);
                pageViewRepository.deleteByProjectId(id);
                sessionRepository.deleteByProjectId(id);
                funnelStepRepository.deleteByProjectId(id);
                funnelRepository.deleteByProjectId(id);
                apiKeyRepository.deleteByProjectId(id);

                projectRepository.delete(project);

                log.info(
                                "Project {} and all associated data deleted by user {}",
                                id,
                                currentUser.getUsername());

                return ProjectResponse.from(project);
        }

        @Transactional(readOnly = true)
        public ProjectSettingsResponse getProjectSettings(Integer projectId, User currentUser) {
                Project project = findProjectById(projectId);
                assertOwnership(project, currentUser);
                return ProjectSettingsResponse.from(project);
        }

        @Transactional
        public ProjectSettingsResponse updateProjectSettings(Integer projectId, ProjectSettingsRequest request, User currentUser) {
                Project project = findProjectById(projectId);
                assertOwnership(project, currentUser);

                if (StringUtils.hasText(request.getDomain())
                                && !request.getDomain().equals(project.getDomain())) {
                        if (projectRepository.existsByDomain(request.getDomain())) {
                                throw new BadRequestException("A project with this domain already exists.");
                        }
                }

                project.setProjectName(request.getProjectName());
                project.setDomain(request.getDomain());
                project.setIndustry(request.getIndustry());
                project.setTimezone(request.getTimezone());

                if (request.getPageviewTracking() != null) {
                        project.setPageviewTracking(request.getPageviewTracking());
                }
                if (request.getSessionRecording() != null) {
                        project.setSessionRecording(request.getSessionRecording());
                }
                if (request.getIpAnonymization() != null) {
                        project.setIpAnonymization(request.getIpAnonymization());
                }
                if (request.getBotFiltering() != null) {
                        project.setBotFiltering(request.getBotFiltering());
                }
                if (request.getCrossDomainTracking() != null) {
                        project.setCrossDomainTracking(request.getCrossDomainTracking());
                }

                project = projectRepository.save(project);
                log.info(
                                "Project {} settings updated by user {}",
                                projectId,
                                currentUser.getUsername());

                return ProjectSettingsResponse.from(project);
        }

        @Transactional
        public ProjectResponse restoreProject(Integer id, User currentUser) {

                Project project = projectRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Project",
                                                "id",
                                                id));

                assertOwnership(project, currentUser);

                project.setProjectStatus(ProjectConstants.ACTIVE);

                project = projectRepository.save(project);

                log.info(
                                "Project {} restored by user {}",
                                id,
                                currentUser.getUsername());

                return ProjectResponse.from(project);
        }

        public ProjectStatsResponse getProjectStats(User currentUser) {

                long totalProjects = projectRepository.countByUserId(currentUser.getId());

                long activeProjects = projectRepository.countByUserIdAndProjectStatus(
                                currentUser.getId(),
                                ProjectConstants.ACTIVE);

                long inactiveProjects = projectRepository.countByUserIdAndProjectStatus(
                                currentUser.getId(),
                                ProjectConstants.INACTIVE);

                long totalPageViews = projectRepository.findByUserId(
                                currentUser.getId(),
                                Pageable.unpaged())
                                .stream()
                                .mapToLong(p -> pageViewRepository.countByProjectId(p.getId()))
                                .sum();

                long totalSessions = projectRepository.findByUserId(
                                currentUser.getId(),
                                Pageable.unpaged())
                                .stream()
                                .mapToLong(p -> sessionRepository.countByProjectId(p.getId()))
                                .sum();

                long totalEvents = projectRepository.findByUserId(
                                currentUser.getId(),
                                Pageable.unpaged())
                                .stream()
                                .mapToLong(p -> eventRepository.countByProjectId(p.getId()))
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

                Project project = projectRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Project",
                                                "id",
                                                id));
                if (!project.getProjectStatus().equals(ProjectConstants.ACTIVE)) {
                        throw new BadRequestException("Project is inactive.");
                }
                return project;
        }

        private void assertOwnership(Project project, User currentUser) {

                if (!project.getUserId().equals(currentUser.getId())) {
                        throw new ForbiddenException(
                                        "You do not have permission to access this project");
                }
        }
}