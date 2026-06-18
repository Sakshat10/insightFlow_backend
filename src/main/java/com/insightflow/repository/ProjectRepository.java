package com.insightflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.insightflow.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    List<Project> findByProjectStatus(Integer projectStatus);

}