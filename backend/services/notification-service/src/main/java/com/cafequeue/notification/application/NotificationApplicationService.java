package com.cafequeue.notification.application;

import com.cafequeue.common.contract.NotificationRequest;
import com.cafequeue.common.core.IdGenerator;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificationApplicationService {
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final JdbcTemplate jdbcTemplate;

    public NotificationApplicationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SseEmitter connect(String studentId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(studentId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(studentId, emitter));
        emitter.onTimeout(() -> remove(studentId, emitter));
        emitter.onError(ignored -> remove(studentId, emitter));
        return emitter;
    }

    public NotificationView send(NotificationRequest request) {
        Instant now = Instant.now();
        NotificationView view = new NotificationView(IdGenerator.prefixed("ntf"), request.studentId(), request.title(), request.content(), request.businessId(), now);
        jdbcTemplate.update("""
                INSERT INTO notification_message(notification_id, student_id, title, content, business_id, status, created_at, sent_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, view.notificationId(), view.studentId(), view.title(), view.content(), view.businessId(), "SENT", Timestamp.from(now), Timestamp.from(now));
        for (SseEmitter emitter : emitters.getOrDefault(request.studentId(), List.of())) {
            try {
                emitter.send(SseEmitter.event().name("cafequeue").data(view));
            } catch (IOException ex) {
                remove(request.studentId(), emitter);
            }
        }
        return view;
    }

    public List<NotificationView> history(String studentId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return jdbcTemplate.query("""
                SELECT notification_id, student_id, title, content, business_id, sent_at
                FROM notification_message
                WHERE student_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """, (rs, rowNum) -> new NotificationView(
                rs.getString("notification_id"),
                rs.getString("student_id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("business_id"),
                rs.getTimestamp("sent_at").toInstant()
        ), studentId, safeLimit);
    }

    private void remove(String studentId, SseEmitter emitter) {
        List<SseEmitter> studentEmitters = emitters.get(studentId);
        if (studentEmitters != null) {
            studentEmitters.remove(emitter);
        }
    }

    public record NotificationView(String notificationId, String studentId, String title, String content, String businessId, Instant sentAt) {
    }
}
