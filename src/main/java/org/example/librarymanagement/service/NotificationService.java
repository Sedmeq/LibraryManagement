package org.example.librarymanagement.service;

public interface NotificationService {
    java.util.concurrent.CompletableFuture<Void> sendLoanConfirmationEmail(String memberEmail, String bookTitle);

}