package com.insightflow.repository;

import com.insightflow.entity.ConversionGoal;
import com.insightflow.entity.ConversionGoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ConversionGoalRepository extends JpaRepository<ConversionGoal, Integer> {

    List<ConversionGoal> findByProjectId(Integer projectId);

    Optional<ConversionGoal> findByProjectIdAndEventName(Integer projectId, String eventName);

    boolean existsByProjectIdAndEventNameAndStatus(Integer projectId, String eventName, ConversionGoalStatus status);

    Optional<ConversionGoal> findByIdAndProjectId(Integer id, Integer projectId);
}
