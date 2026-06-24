package com.insightflow.repository;

import com.insightflow.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Page<Session> findByProjectId(Integer projectId, Pageable pageable);

    Optional<Session> findBySessionId(String sessionId);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.projectId = :projectId")
    long countByProjectId(@Param("projectId") Integer projectId);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.projectId = :projectId AND s.startedAt >= :since")
    long countByProjectIdAndStartedAtAfter(
            @Param("projectId") Integer projectId,
            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT s.ipAddress) FROM Session s WHERE s.projectId = :projectId")
    long countDistinctIpByProjectId(@Param("projectId") Integer projectId);

    @Query("SELECT AVG(s.duration) FROM Session s WHERE s.projectId = :projectId AND s.duration IS NOT NULL")
    Double avgDurationByProjectId(@Param("projectId") Integer projectId);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.projectId = :projectId AND s.isBounce = true")
    long countBouncedByProjectId(@Param("projectId") Integer projectId);

    @Query("""
            SELECT s.deviceType, COUNT(s)
            FROM Session s
            WHERE s.projectId = :projectId
            GROUP BY s.deviceType
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> countByDeviceTypeAndProjectId(@Param("projectId") Integer projectId);

    @Query("""
            SELECT s.browser, COUNT(s)
            FROM Session s
            WHERE s.projectId = :projectId
            GROUP BY s.browser
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> countByBrowserAndProjectId(@Param("projectId") Integer projectId);

    @Query("""
            SELECT s.country, COUNT(s)
            FROM Session s
            WHERE s.projectId = :projectId
            GROUP BY s.country
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> countByCountryAndProjectId(@Param("projectId") Integer projectId);

    @Query("""
            SELECT s.referrer, COUNT(s)
            FROM Session s
            WHERE s.projectId = :projectId
              AND s.referrer IS NOT NULL
              AND s.referrer <> ''
            GROUP BY s.referrer
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> countByReferrerAndProjectId(@Param("projectId") Integer projectId);

    @Query("""
            SELECT CAST(s.startedAt AS date), COUNT(s), COUNT(s)
            FROM Session s
            WHERE s.projectId = :projectId
              AND s.startedAt BETWEEN :from AND :to
            GROUP BY CAST(s.startedAt AS date)
            ORDER BY CAST(s.startedAt AS date)
            """)
    List<Object[]> dailySessionsByProjectId(
            @Param("projectId") Integer projectId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}