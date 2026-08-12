package org.example.librarymanagement.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.librarymanagement.service.NotificationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Async("notificationExecutor")
    @Override
    public CompletableFuture<Void> sendLoanConfirmationEmail(String memberEmail, String bookTitle) {
        try {
            Thread.sleep(2000);
            log.info("Email göndərildi: {} → '{}' kitabının icarəsi", memberEmail, bookTitle);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return CompletableFuture.completedFuture(null);
    }
}