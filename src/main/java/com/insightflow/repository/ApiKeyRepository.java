package com.insightflow.repository;

import com.insightflow.entity.ApiKey;
import com.insightflow.entity.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByProjectIdOrderByCreatedAtDescIdDesc(Integer projectId);

    long countByProjectId(Integer projectId);

    long countByProjectIdAndStatus(Integer projectId, ApiKeyStatus status);

    @Query("SELECT COALESCE(SUM(a.requestCount), 0) FROM ApiKey a WHERE a.projectId = :projectId")
    long sumRequestCountByProjectId(@Param("projectId") Integer projectId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ApiKey a SET a.requestCount = a.requestCount + 1, a.lastUsedAt = CURRENT_TIMESTAMP WHERE a.id = :id AND a.status = 'ACTIVE'")
    int incrementRequestCount(@Param("id") Integer id);

    @Modifying
    @Query("DELETE FROM ApiKey a WHERE a.projectId = :projectId")
    void deleteByProjectId(@Param("projectId") Integer projectId);
}
