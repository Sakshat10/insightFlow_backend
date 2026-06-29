package com.insightflow.repository;

import com.insightflow.entity.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            SELECT e
            FROM Event e
            JOIN Session s
                ON e.sessionId = s.id
            WHERE s.projectId = :projectId
            """)
    List<Event> findByProjectId(
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
}