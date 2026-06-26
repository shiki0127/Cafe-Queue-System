package com.cafequeue.queue.interfaces;

import com.cafequeue.common.contract.QueueDispatchRequest;
import com.cafequeue.common.contract.QueueDispatchResponse;
import com.cafequeue.common.core.ApiResponse;
import com.cafequeue.queue.application.QueueApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/queues")
public class QueueController {
    private final QueueApplicationService queueService;

    public QueueController(QueueApplicationService queueService) {
        this.queueService = queueService;
    }

    @PostMapping("/dispatch")
    public ApiResponse<QueueDispatchResponse> dispatch(@RequestBody QueueDispatchRequest request) {
        return ApiResponse.ok(queueService.dispatch(request));
    }

    @GetMapping("/machines/{machineId}")
    public ApiResponse<List<QueueApplicationService.QueueTicketView>> machineQueue(@PathVariable String machineId) {
        return ApiResponse.ok(queueService.machineQueue(machineId));
    }

    @PostMapping("/{ticketId}/complete")
    public ApiResponse<QueueApplicationService.QueueTicketView> complete(@PathVariable String ticketId) {
        return ApiResponse.ok(queueService.complete(ticketId));
    }
}
