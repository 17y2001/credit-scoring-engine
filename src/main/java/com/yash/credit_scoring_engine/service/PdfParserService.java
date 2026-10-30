package com.yash.credit_scoring_engine.service;

import com.yash.credit_scoring_engine.model.Transaction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.Loader;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfParserService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy");

    public List<Transaction> parseBankStatement(InputStream pdfStream, String userId) throws Exception {
        List<Transaction> transactions = new ArrayList<>();

        PDDocument document = Loader.loadPDF(pdfStream.readAllBytes());
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();

        System.out.println("=== PDF TEXT START ===");
        System.out.println(text.substring(0, Math.min(2000, text.length())));
        System.out.println("=== PDF TEXT END ===");

// DEBUG: check first matching line
        for (String line : text.split("\\n")) {
            line = line.trim();
            if (line.matches("^\\d{2}/\\d{2}/\\d{2}.*")) {
                System.out.println("MATCHED LINE: [" + line + "]");
                String[] parts = line.split("\\s{2,}");
                System.out.println("PARTS COUNT: " + parts.length);
                for (int i = 0; i < parts.length; i++) {
                    System.out.println("  parts[" + i + "] = [" + parts[i] + "]");
                }
                break; // sirf pehli line dekho
            }
        }

        // Pattern: date | narration | ref | value date | withdrawal | deposit | balance
        String[] lines = text.split("\\n");
        for (String line : lines) {
            line = line.trim();

            if (!line.matches("^\\d{2}/\\d{2}/\\d{2}.*")) continue;

            // Single space split
            String[] parts = line.split("\\s+");

            // Minimum parts needed: date, narration, ref, valuedate, amount, balance = 6
            if (parts.length < 6) continue;

            try {
                String date = parts[0];  // DD/MM/YY

                // Last part = closing balance
                // Second last = transaction amount (withdrawal OR deposit)
                String closingBalance = parts[parts.length - 1];
                String txnAmount = parts[parts.length - 2];

                // txnAmount must be a valid number like 14,955.00
                if (!txnAmount.matches("[\\d,]+\\.\\d+")) continue;

                // Narration = everything between date and ref number
                // Ref number = second last alphanumeric block before value date
                // Value date = second occurrence of DD/MM/YY pattern
                // Build narration by collecting parts until we hit the ref number
                StringBuilder narration = new StringBuilder();
                int refIndex = -1;
                for (int i = 1; i < parts.length - 3; i++) {
                    // Ref number is long alphanumeric, usually 12-16 chars
                    if (parts[i].matches("[A-Z0-9]{10,}") && refIndex == -1) {
                        refIndex = i;
                        break;
                    }
                    narration.append(parts[i]).append(" ");
                }

                if (refIndex == -1) continue; // no ref found, skip

                // Determine CREDIT or DEBIT from narration keywords
                String narrationStr = narration.toString().trim();
                String upper = narrationStr.toUpperCase();
                String type;
                if (upper.contains("CREDIT") || upper.contains("INTEREST") ||
                        upper.contains("CASH DEP") || upper.contains("UPI") && txnAmount.equals(parts[parts.length-2])) {
                    // UPI can be both - need more context, default to checking value date position
                    // For now: CREDIT keywords = CREDIT, rest = DEBIT
                    type = "CREDIT";
                } else {
                    type = "DEBIT";
                }

                // Better: check if value date (parts[refIndex+1]) matches DD/MM/YY
                // Then parts[refIndex+2] onwards are amounts
                if (refIndex + 1 < parts.length && parts[refIndex+1].matches("\\d{2}/\\d{2}/\\d{2}")) {
                    // After value date: remaining parts are withdrawal/deposit/balance
                    int afterValueDate = refIndex + 2;
                    int remainingAmounts = parts.length - afterValueDate;

                    if (remainingAmounts == 2) {
                        // Only 1 amount + balance = single column (withdrawal OR deposit)
                        // Can't tell which without context — use narration
                        type = (upper.contains("CREDIT") || upper.contains("INTEREST") ||
                                upper.contains("CASH DEP")) ? "CREDIT" : "DEBIT";
                    } else if (remainingAmounts == 3) {
                        // withdrawal + deposit + balance OR one blank
                        // parts[afterValueDate] = withdrawal, parts[afterValueDate+1] = deposit
                        String maybeWithdrawal = parts[afterValueDate];
                        String maybeDeposit = parts[afterValueDate + 1];
                        if (maybeDeposit.matches("[\\d,]+\\.\\d+")) {
                            txnAmount = maybeDeposit;
                            type = "CREDIT";
                        } else {
                            txnAmount = maybeWithdrawal;
                            type = "DEBIT";
                        }
                    }
                }

                Transaction t = new Transaction();
                t.setUserId(userId);
                t.setDate(LocalDate.parse(date, DATE_FORMAT));
                t.setDescription(narrationStr);
                t.setAmount(Double.parseDouble(txnAmount.replace(",", "")));
                t.setType(type);
                transactions.add(t);

            } catch (Exception e) {
                // skip
            }
        }

        System.out.println("Parsed " + transactions.size() + " transactions from PDF");
        return transactions;
    }
}