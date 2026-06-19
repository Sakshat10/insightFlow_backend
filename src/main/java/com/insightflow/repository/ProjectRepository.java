package com.insightflow.repository;

import com.insightflow.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    Page<Project> findAll(Pageable pageable);

    Page<Project> findByProjectStatus(
            Integer projectStatus,
            Pageable pageable);

    Page<Project> findByUserId(
            Integer userId,
            Pageable pageable);

    Page<Project> findByUserIdAndProjectStatus(
            Integer userId,
            Integer projectStatus,
            Pageable pageable);

    Optional<Project> findById(Integer id);

    Optional<Project> findByTrackingKey(String trackingKey);

    boolean existsByTrackingKey(String trackingKey);

    long countByUserId(Integer userId);

    long countByUserIdAndProjectStatus(
            Integer userId,
            Integer projectStatus);
}