package com.insightflow.repository;

import com.insightflow.entity.Funnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FunnelRepository extends JpaRepository<Funnel, Integer> {

    @Query("""
            SELECT DISTINCT f
            FROM Funnel f
            LEFT JOIN FETCH f.steps
            WHERE f.projectId = :projectId
            """)
    List<Funnel> findByProjectIdWithSteps(@Param("projectId") Integer projectId);

    @Query("""
            SELECT f
            FROM Funnel f
            LEFT JOIN FETCH f.steps
            WHERE f.id = :id
            """)
    Optional<Funnel> findByIdWithSteps(@Param("id") Integer id);

    @Modifying
    @Query("DELETE FROM Funnel f WHERE f.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") Integer projectId);
}
