package com.insightflow.repository;

import com.insightflow.dto.PageViewProjection;
import com.insightflow.entity.PageView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PageViewRepository extends JpaRepository<PageView, Long> {

    @Query("""
            SELECT
                pv.id AS id,
                s.projectId AS projectId,
                pv.sessionId AS sessionId,
                pv.url AS url,
                pv.title AS title,
                pv.referrer AS referrer,
                pv.createdAt AS createdAt
            FROM PageView pv
            JOIN Session s
                ON pv.sessionId = s.id
            WHERE s.projectId = :projectId
            """)
    List<PageViewProjection> findByProjectId(
            @Param("projectId") Integer projectId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(pv)
            FROM PageView pv
            JOIN Session s
                ON pv.sessionId = s.id
            WHERE s.projectId = :projectId
            """)
    long countByProjectId(
            @Param("projectId") Integer projectId);

    @Query("""
            SELECT COUNT(pv)
            FROM PageView pv
            JOIN Session s
                ON pv.sessionId = s.id
            WHERE s.projectId = :projectId
              AND pv.createdAt >= :since
            """)
    long countByProjectIdAndCreatedAtAfter(
            @Param("projectId") Integer projectId,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT pv.url, COUNT(pv)
            FROM PageView pv
            JOIN Session s
                ON pv.sessionId = s.id
            WHERE s.projectId = :projectId
            GROUP BY pv.url
            ORDER BY COUNT(pv) DESC
            """)
    List<Object[]> topPagesByProjectId(
            @Param("projectId") Integer projectId,
            Pageable pageable);

    @Query("""
            SELECT CAST(pv.createdAt AS date), COUNT(pv)
            FROM PageView pv
            JOIN Session s
                ON pv.sessionId = s.id
            WHERE s.projectId = :projectId
              AND pv.createdAt BETWEEN :from AND :to
            GROUP BY CAST(pv.createdAt AS date)
            ORDER BY CAST(pv.createdAt AS date)
            """)
    List<Object[]> dailyPageViewsByProjectId(
            @Param("projectId") Integer projectId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}