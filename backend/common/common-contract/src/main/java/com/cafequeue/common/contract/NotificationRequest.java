package com.cafequeue.common.contract;

public record NotificationRequest(String studentId, String title, String content, String businessId) {
}
