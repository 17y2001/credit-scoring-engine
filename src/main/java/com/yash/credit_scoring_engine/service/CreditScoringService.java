package com.yash.credit_scoring_engine.service;

import com.yash.credit_scoring_engine.model.CreditReport;
import com.yash.credit_scoring_engine.model.Transaction;
import com.yash.credit_scoring_engine.repository.CreditReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreditScoringService {

    @Autowired
    private CreditReportRepository creditReportRepository ;

    public CreditReport calculateScore(String userId, List<Transaction> transactions){
        double totalCredits = transactions.stream()
                .filter(t -> t.getType().equals("CREDIT"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalDebits = transactions.stream()
                .filter(t -> t.getType().equals("DEBIT"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double avgMonthlyBalance = totalCredits - totalDebits ;

        int score = calculateCreditScore(totalCredits, totalDebits, transactions);
        String rating = getRating(score) ;

        CreditReport report = new CreditReport();
        report.setUserId(userId);
        report.setCreditScore(score);
        report.setRating(rating);
        report.setTotalCredits(totalCredits);
        report.setTotalDebits(totalDebits);
        report.setAvgMonthlyBalance(avgMonthlyBalance);
        report.setGeneratedAt(LocalDateTime.now()) ;

        return creditReportRepository.save(report);
    }

    private int calculateCreditScore(double credits, double debits, List<Transaction> transactions){
        int score = 300;

        // Factor 1: Income level (max 200 points)
        if (credits > 100000) score += 200;
        else if (credits > 50000) score += 150;
        else if (credits > 20000) score += 100;
        else score += 50;

        // Factor 2: FOIR - Fixed Obligation to Income Ratio (max 200 points)
        double foir = debits / credits;
        if (foir < 0.3) score += 200;
        else if (foir < 0.5) score += 150;
        else if (foir < 0.7) score += 100;
        else score += 30;

        // Factor 3: Transaction count - financial activity (max 100 points)
        if (transactions.size() > 20) score += 100;
        else if (transactions.size() > 10) score += 70;
        else score += 30;

        // Factor 4: Savings ratio (max 100 points)
        double savingsRatio = (credits - debits) / credits;
        if (savingsRatio > 0.3) score += 100;
        else if (savingsRatio > 0.1) score += 60;
        else score += 20;

        return Math.min(score, 900);
    }

    private String getRating(int score) {
        if (score >= 750) return "EXCELLENT";
        else if (score >= 650) return "GOOD";
        else if (score >= 550) return "FAIR";
        else return "POOR";
    }

}
