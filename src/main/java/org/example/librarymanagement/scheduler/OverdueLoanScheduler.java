package org.example.librarymanagement.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.librarymanagement.entity.Loan;
import org.example.librarymanagement.entity.LoanStatus;
import org.example.librarymanagement.repository.LoanRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled tasks for daily loan maintenance.
 *
 * <p>Task 1 — markOverdueLoans: Runs every night at 01:00.
 * Finds all ACTIVE loans whose dueDate is before today and marks them OVERDUE.
 *
 * <p>Task 2 — cleanOldReturnedLoans: Runs every night at 02:00.
 * Deletes RETURNED loan records older than 365 days to keep the table lean.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueLoanScheduler {

    private final LoanRepository loanRepository;

    /** Marks active loans past their due date as OVERDUE. Runs daily at 01:00. */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void markOverdueLoans() {
        List<Loan> overdue = loanRepository.findOverdueLoans(LocalDate.now());
        overdue.forEach(loan -> loan.setStatus(LoanStatus.OVERDUE));
        loanRepository.saveAll(overdue);
        log.info("[Scheduler] markOverdueLoans: {} icarə OVERDUE olaraq işarələndi", overdue.size());
    }

    /**
     * Deletes RETURNED loan records older than 1 year. Runs daily at 02:00.
     * This is the daily cleaning task — keeps historical data manageable.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanOldReturnedLoans() {
        LocalDate cutoff = LocalDate.now().minusDays(365);
        int deleted = loanRepository.deleteReturnedLoansBefore(cutoff);
        log.info("[Scheduler] cleanOldReturnedLoans: {} köhnə RETURNED icarə silindi (cutoff={})",
                deleted, cutoff);
    }
}
