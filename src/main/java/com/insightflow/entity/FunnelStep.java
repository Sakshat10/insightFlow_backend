package com.insightflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "funnel_steps")
public class FunnelStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "funnel_id", nullable = false, insertable = false, updatable = false)
    private Integer funnelId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "event_name", nullable = false, length = 255)
    private String eventName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
