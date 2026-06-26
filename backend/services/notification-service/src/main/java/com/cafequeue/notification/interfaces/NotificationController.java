package com.cafequeue.notification.interfaces;

import com.cafequeue.common.contract.NotificationRequest;
import com.cafequeue.common.core.ApiResponse;
import com.cafequeue.notification.application.NotificationApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationApplicationService notificationService;

    public NotificationController(NotificationApplicationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/stream/{studentId}")
    public SseEmitter stream(@PathVariable String studentId) {
        return notificationService.connect(studentId);
    }

    @PostMapping
    public ApiResponse<NotificationApplicationService.NotificationView> send(@RequestBody NotificationRequest request) {
        return ApiResponse.ok(notificationService.send(request));
    }

    @GetMapping("/students/{studentId}")
    public ApiResponse<List<NotificationApplicationService.NotificationView>> history(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(notificationService.history(studentId, limit));
    }
}
