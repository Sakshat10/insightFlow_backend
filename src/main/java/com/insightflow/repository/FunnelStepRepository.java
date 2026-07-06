package com.insightflow.repository;

import com.insightflow.entity.FunnelStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FunnelStepRepository extends JpaRepository<FunnelStep, Integer> {
}
