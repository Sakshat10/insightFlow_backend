package com.insightflow.repository;

import com.insightflow.dto.EventTimelineProjection;
import com.insightflow.entity.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            SELECT
                e.id AS id,
                e.sessionId AS sessionId,
                e.eventName AS eventName,
                e.eventCategory AS eventCategory,
                e.eventLabel AS eventLabel,
                e.eventValue AS eventValue,
                e.url AS url,
                e.properties AS properties,
                e.isConversion AS isConversion,
                s.country AS country,
                s.browser AS browser,
                s.deviceType AS deviceType,
                e.createdAt AS createdAt,
                e.updatedAt AS updatedAt
            FROM Event e
            JOIN Session s
                ON e.sessionId = s.id
            WHERE s.projectId = :projectId
            """)
    List<com.insightflow.dto.EventDetailsProjection> findByProjectId(
            @Param("projectId") Integer projectId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(e)
            FROM Event e
            JOIN Session s
                ON e.sessionId = s.id
            WHERE s.projectId = :projectId
            """)
    long countByProjectId(
            @Param("projectId") Integer projectId);

    @Query("""
            SELECT e.eventName, COUNT(e)
            FROM Event e
            JOIN Session s
                ON e.sessionId = s.id
            WHERE s.projectId = :projectId
            GROUP BY e.eventName
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> countByEventNameAndProjectId(
            @Param("projectId") Integer projectId,
            Pageable pageable);

    @Query(value = """
            SELECT
                DATE(e.created_at) AS date,
                e.event_name AS event_name,
                COUNT(*) AS count
            FROM events e
            JOIN sessions s
                ON e.session_id = s.id
            WHERE s.project_id = :projectId
              AND e.created_at BETWEEN :from AND :to
            GROUP BY DATE(e.created_at), e.event_name
            ORDER BY DATE(e.created_at) ASC
            """, nativeQuery = true)
    List<EventTimelineProjection> getEventTimeline(
            @Param("projectId") Integer projectId,
            @Param("from") String from,
            @Param("to") String to);

    @Query(value = """
        SELECT COUNT(*)
        FROM events e
        JOIN sessions s
            ON e.session_id = s.id
        WHERE s.project_id = :projectId
          AND e.created_at BETWEEN :from AND :to
        """, nativeQuery = true)
    long debugCount(
            @Param("projectId") Integer projectId,
            @Param("from") String from,
            @Param("to") String to);

    @Query("""
            SELECT
                e.id AS id,
                e.sessionId AS sessionId,
                e.eventName AS eventName,
                e.createdAt AS createdAt
            FROM Event e
            JOIN Session s
                ON e.sessionId = s.id
            WHERE s.projectId = :projectId
              AND e.createdAt >= :fromDateTime
              AND e.createdAt < :toExclusive
              AND e.eventName IN :stepNames
            ORDER BY
                e.sessionId ASC,
                e.createdAt ASC,
                e.id ASC
            """)
    List<com.insightflow.dto.FunnelEventProjection> findFunnelEvents(
            @Param("projectId") Integer projectId,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toExclusive") LocalDateTime toExclusive,
            @Param("stepNames") List<String> stepNames);
}