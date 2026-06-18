package com.insightflow.service;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.insightflow.constants.ProjectConstants;
import com.insightflow.dto.CreateProjectRequest;
import com.insightflow.dto.UpdateProjectRequest;
import com.insightflow.entity.Project;
import com.insightflow.repository.ProjectRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void createProject(CreateProjectRequest request) {

        Project project = new Project();

        project.setProjectName(request.getProjectName());
        project.setDomain(request.getDomain());
        project.setUserId(1);
        project.setProjectStatus(ProjectConstants.ACTIVE);
        project.setTrackingKey("trk_1231");

        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        projectRepository.save(project);
        
        
    }


    public List<Project> getAllProjects(Integer projectStatus) {
        if(projectStatus == null){
            return projectRepository.findAll();
        }
        return projectRepository.findByProjectStatus(projectStatus);
    }

    public Project getProjectById(Integer id){
        return projectRepository.findById(id).orElse(null);
    }

    public Project updateProject(Integer id, UpdateProjectRequest request){

        Project project = projectRepository.findById(id).orElse(null);

        if (project == null){
            return null;
        }

        if (request.getProjectName() != null){
            project.setProjectName(request.getProjectName());
        }

        if (request.getDomain() != null){
            project.setDomain(request.getDomain());
        }

        if (request.getProjectStatus() != null){
            project.setProjectStatus(request.getProjectStatus());
        }

        return projectRepository.save(project);

    }

    
    public Project deleteProject(Integer id){
        Project project = projectRepository.findById(id).orElse(null);

        if(project == null){
            return null;
        }

        project.setProjectStatus(ProjectConstants.INACTIVE);
        
        return projectRepository.save(project);

    }

}