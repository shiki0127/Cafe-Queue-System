package com.cafequeue.common.contract;

public record QueueDispatchResponse(String queueTicketId, int queuePosition, long estimatedWaitSeconds) {
}
