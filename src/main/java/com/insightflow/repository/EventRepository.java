package com.insightflow.repository;

import com.insightflow.dto.EventTimelineProjection;
import com.insightflow.entity.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("""
            SELECT COUNT(e) > 0
            FROM Event e
            JOIN Session s
                ON e.sessionId = s.id
            WHERE s.projectId = :projectId
              AND e.eventName = :eventName
            """)
    boolean existsByProjectIdAndEventName(
            @Param("projectId") Integer projectId,
            @Param("eventName") String eventName);

    @Query(value = """
            SELECT 
                e.event_name AS eventName,
                COUNT(e.id) AS count,
                COUNT(DISTINCT s.visitor_id) AS uniqueUsers,
                MAX(e.event_category) AS category,
                MAX(e.created_at) AS lastSeen,
                COUNT(DISTINCT e.session_id) AS distinctSessions
            FROM events e
            JOIN sessions s ON e.session_id = s.id
            WHERE s.project_id = :projectId
              AND e.created_at BETWEEN :from AND :to
            GROUP BY e.event_name
            ORDER BY count DESC
            """, nativeQuery = true)
    List<Object[]> findEventAnalyticsInPeriod(
            @Param("projectId") Integer projectId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    @Query(value = """
            SELECT 
                e.event_name AS eventName,
                COUNT(e.id) AS count
            FROM events e
            JOIN sessions s ON e.session_id = s.id
            WHERE s.project_id = :projectId
              AND e.created_at BETWEEN :from AND :to
              AND e.event_name IN :eventNames
            GROUP BY e.event_name
            """, nativeQuery = true)
    List<Object[]> findEventCountsInPeriod(
            @Param("projectId") Integer projectId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("eventNames") List<String> eventNames);

    @Modifying
    @Query(value = "DELETE FROM events WHERE session_id IN (SELECT id FROM sessions WHERE project_id = :projectId)", nativeQuery = true)
    void deleteByProjectId(@Param("projectId") Integer projectId);

    @Query(value = """
            SELECT 
                DATE(e.created_at) AS dateStr,
                COUNT(e.id) AS countVal
            FROM events e
            JOIN sessions s ON e.session_id = s.id
            WHERE s.project_id = :projectId
              AND e.is_conversion = true
              AND e.created_at BETWEEN :from AND :to
            GROUP BY DATE(e.created_at)
            ORDER BY DATE(e.created_at) ASC
            """, nativeQuery = true)
    List<Object[]> getConversionTimeline(
            @Param("projectId") Integer projectId,
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Event e
            SET e.isConversion = :isConversion
            WHERE e.sessionId IN (
                SELECT s.id FROM Session s WHERE s.projectId = :projectId
            )
            AND e.eventName = :eventName
            """)
    int updateConversionStatusByProjectAndEventName(
            @Param("projectId") Integer projectId,
            @Param("eventName") String eventName,
            @Param("isConversion") boolean isConversion);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Event e
            SET e.isConversion = :isConversion
            WHERE e.sessionId IN (
                SELECT s.id FROM Session s WHERE s.projectId = :projectId
            )
            """)
    int updateConversionStatusByProject(
            @Param("projectId") Integer projectId,
            @Param("isConversion") boolean isConversion);

    @Query("""
            SELECT e
            FROM Event e
            JOIN Session s ON e.sessionId = s.id
            WHERE s.projectId = :projectId
            ORDER BY e.createdAt DESC
            """)
    List<Event> findRecentByProjectId(
            @Param("projectId") Integer projectId,
            Pageable pageable);
}