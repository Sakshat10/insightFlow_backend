package com.insightflow.service;

import com.insightflow.dto.CreatePageViewRequest;
import com.insightflow.dto.PageViewResponse;
import com.insightflow.dto.PagedResponse;
import com.insightflow.entity.PageView;
import com.insightflow.entity.Project;
import com.insightflow.entity.User;
import com.insightflow.exception.BadRequestException;
import com.insightflow.exception.ResourceNotFoundException;
import com.insightflow.repository.PageViewRepository;
import com.insightflow.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PageViewService {

    private final PageViewRepository pageViewRepository;
    private final ProjectRepository projectRepository;

    public PageViewService(PageViewRepository pageViewRepository,
                           ProjectRepository projectRepository) {
        this.pageViewRepository = pageViewRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public PageViewResponse createPageView(CreatePageViewRequest request,
                                           User currentUser) {

        Project project = projectRepository.findByTrackingKey(
                        request.getTrackingKey())
                .orElseThrow(() ->
                        new BadRequestException("Invalid tracking key"));

        if (!project.getProjectStatus().equals(1)) {
            throw new BadRequestException("Project is inactive");
        }

        PageView pageView = PageView.builder()
                .sessionId(Integer.valueOf(request.getSessionId()))
                .url(request.getUrl())
                .title(request.getTitle())
                .referrer(request.getReferrer())
                .build();

        pageView = pageViewRepository.save(pageView);

        log.info(
                "PageView recorded for session {}",
                pageView.getSessionId()
        );

        return PageViewResponse.from(pageView);
    }

    public PagedResponse<PageViewResponse> getPageViews(Integer projectId,
                                                        int page,
                                                        int size,
                                                        String sortBy,
                                                        String sortDir,
                                                        User currentUser) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PageViewResponse> pageResult =
                new PageImpl<>(
                        pageViewRepository.findByProjectId(projectId, pageable)
                                .stream()
                                .map(PageViewResponse::from)
                                .toList(),
                        pageable,
                        pageViewRepository.countByProjectId(projectId)
                );

        return PagedResponse.of(pageResult);
    }

    public PageViewResponse getPageViewById(Long id,
                                            User currentUser) {

        PageView pageView = pageViewRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "PageView",
                                "id",
                                id
                        ));

        return PageViewResponse.from(pageView);
    }
}