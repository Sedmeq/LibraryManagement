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

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueLoanScheduler {

    private final LoanRepository loanRepository;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void markOverdueLoans() {
        List<Loan> overdue = loanRepository.findOverdueLoans(LocalDate.now());
        overdue.forEach(loan -> loan.setStatus(LoanStatus.OVERDUE));
        loanRepository.saveAll(overdue);
        log.info("Scheduled task: {} icarə OVERDUE olaraq işarələndi", overdue.size());
    }
}