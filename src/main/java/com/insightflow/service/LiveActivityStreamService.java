package com.insightflow.service;

import com.insightflow.dto.LiveActivityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
public class LiveActivityStreamService {

    private final Map<Integer, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Integer projectId) {
        SseEmitter emitter = new SseEmitter(1800000L); // 30 minutes

        emitters.computeIfAbsent(projectId, k -> new CopyOnWriteArraySet<>()).add(emitter);
        log.debug("SSE subscriber connected: projectId={}, active subscriber count={}",
                projectId, emitters.get(projectId).size());

        emitter.onCompletion(() -> removeEmitter(projectId, emitter, "completed"));
        emitter.onTimeout(() -> removeEmitter(projectId, emitter, "timeout"));
        emitter.onError((ex) -> removeEmitter(projectId, emitter, "error"));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("projectId", projectId, "status", "CONNECTED")));
        } catch (IOException e) {
            log.debug("Initial connection notification failed for project {}: {}", projectId, e.getMessage());
            removeEmitter(projectId, emitter, "initial send failure");
        }

        return emitter;
    }

    public void publish(Integer projectId, LiveActivityResponse activity) {
        Set<SseEmitter> projectEmitters = emitters.get(projectId);
        if (projectEmitters == null || projectEmitters.isEmpty()) {
            log.debug("Activity published: projectId={}, activityId={}, type={}, subscriber count=0",
                    projectId, activity.getActivityId(), activity.getType());
            return;
        }

        log.debug("Activity published: projectId={}, activityId={}, type={}, subscriber count={}",
                projectId, activity.getActivityId(), activity.getType(), projectEmitters.size());

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : projectEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(activity.getActivityId())
                        .name("activity")
                        .data(activity));
            } catch (Exception e) {
                log.debug("Send failure: projectId={}, activityId={}: {}", projectId, activity.getActivityId(), e.getMessage());
                deadEmitters.add(emitter);
            }
        }

        for (SseEmitter dead : deadEmitters) {
            removeEmitter(projectId, dead, "send failure");
        }
    }

    public void removeEmitter(Integer projectId, SseEmitter emitter, String reason) {
        Set<SseEmitter> projectEmitters = emitters.get(projectId);
        if (projectEmitters != null) {
            if (projectEmitters.remove(emitter)) {
                log.debug("SSE subscriber removed: projectId={}, reason={}, remaining count={}",
                        projectId, reason, projectEmitters.size());
            }
            if (projectEmitters.isEmpty()) {
                emitters.remove(projectId);
            }
        }
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        Map<String, String> heartbeatData = Map.of("timestamp", LocalDateTime.now().toString());

        emitters.forEach((projectId, projectEmitters) -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : projectEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(heartbeatData));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            for (SseEmitter dead : deadEmitters) {
                removeEmitter(projectId, dead, "heartbeat failure");
            }
        });
    }

    public int getSubscriberCount(Integer projectId) {
        Set<SseEmitter> projectEmitters = emitters.get(projectId);
        return projectEmitters != null ? projectEmitters.size() : 0;
    }
}
