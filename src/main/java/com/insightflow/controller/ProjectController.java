package com.insightflow.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.insightflow.dto.CreateProjectRequest;
import com.insightflow.dto.UpdateProjectRequest;
import com.insightflow.entity.Project;
import com.insightflow.service.ProjectService;

@RestController
public class ProjectController {
    
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/projects")
    public void createProject(@RequestBody CreateProjectRequest request){
        projectService.createProject(request);
    }

    @GetMapping("/projects")
    public List<Project> getAllProjects(@RequestParam(required = false) Integer projectStatus) {
        return projectService.getAllProjects(projectStatus);
    }

    @GetMapping("/projects/{id}")
    public Project getProjectById(@PathVariable Integer id) {
        return projectService.getProjectById(id);
    }

    @PutMapping("/projects/{id}")
    public Project updateProject(@PathVariable Integer id, @RequestBody UpdateProjectRequest request){
        return projectService.updateProject(id, request);
    }

    @DeleteMapping("/projects/{id}")
    public Project deleteProject(@PathVariable Integer id){
        return projectService.deleteProject(id);
    }
}
