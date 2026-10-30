package com.yash.credit_scoring_engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yash.credit_scoring_engine.model.CreditReport;
import com.yash.credit_scoring_engine.model.Transaction;
import com.yash.credit_scoring_engine.repository.CreditReportRepository;
import com.yash.credit_scoring_engine.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.yash.credit_scoring_engine.service.PdfParserService;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/credit")
public class CreditScoringController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private CreditReportRepository creditReportRepository;

    @Autowired
    private PdfParserService pdfParserService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

    @PostMapping("/upload/{userId}")
    public String uploadPdf(@PathVariable String userId,
                            @RequestParam("file") MultipartFile file) {
        try {
            List<Transaction> transactions = pdfParserService.parseBankStatement(
                    file.getInputStream(), userId);

            if (transactions.isEmpty()) {
                return "No transactions found in PDF!";
            }

            String transactionsJson = objectMapper.writeValueAsString(transactions);
            kafkaProducerService.sendForScoring(userId, transactionsJson);

            return "PDF uploaded! Found " + transactions.size() +
                    " transactions for userId: " + userId + ". Score is being calculated...";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}