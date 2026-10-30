package com.yash.credit_scoring_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yash.credit_scoring_engine.model.CreditReport;
import com.yash.credit_scoring_engine.model.Transaction;
import com.yash.credit_scoring_engine.repository.CreditReportRepository;
import com.yash.credit_scoring_engine.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/credit")
public class CreditScoringController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private CreditReportRepository creditReportRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/analyze/{userId}")
    public String analyzeStatement(
            @PathVariable String userId,
            @RequestBody List<Transaction> transactions) {
        try {
            String transactionsJson = objectMapper.writeValueAsString(transactions);
            kafkaProducerService.sendForScoring(userId, transactionsJson);
            return "Statement received for userId: " + userId + ". Score is being calculated...";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/score/{userId}")
    public String getScore(@PathVariable String userId) {
        Optional<CreditReport> report = creditReportRepository
                .findTopByUserIdOrderByGeneratedAtDesc(userId);

        if (report.isPresent()) {
            CreditReport r = report.get();
            return String.format(
                    "userId: %s | Score: %d | Rating: %s | Credits: %.0f | Debits: %.0f",
                    r.getUserId(), r.getCreditScore(), r.getRating(),
                    r.getTotalCredits(), r.getTotalDebits()
            );
        } else {
            return "No credit report found for userId: " + userId;
        }
    }
}