package com.example.statsservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.statsservice.model.Account;
import com.example.statsservice.model.AccountStats;
import com.example.statsservice.model.Document;
import com.example.statsservice.model.DocumentStats;
import com.example.statsservice.repository.AccountRepository;
import com.example.statsservice.repository.AccountStatsRepository;
import com.example.statsservice.repository.DocumentRepository;
import com.example.statsservice.repository.DocumentStatsRepository;
import com.example.statsservice.enums.Role;
import com.example.statsservice.enums.DocumentFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StatsService {

    @Autowired
    private AccountStatsRepository accountStatsRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DocumentStatsRepository documentStatsRepository;

    @Autowired
    private DocumentRepository documentRepository;

    // ============ ACCOUNT METHODS ============

    @Transactional
    public void saveAccountEvent(Account newAccount) {
        Optional<Account> existingAccountOpt = accountRepository.findById(newAccount.getId());
        AccountStats stats = getCurrentAccountStats();

        if (existingAccountOpt.isPresent()) {
            // UPDATE: Kiểm tra xem role có thay đổi không
            Account existingAccount = existingAccountOpt.get();
            Role oldRole = existingAccount.getRole();
            Role newRole = newAccount.getRole();

            if (oldRole != newRole) {
                // Trừ 1 từ role cũ
                decrementRoleCount(stats, oldRole);
                // Cộng 1 vào role mới
                incrementRoleCount(stats, newRole);

                stats.setLastUpdated(LocalDateTime.now());
                accountStatsRepository.save(stats);

                System.out.println("Account role changed from " + oldRole + " to " + newRole);
            }

            // Cập nhật account
            existingAccount.setUsername(newAccount.getUsername());
            existingAccount.setRole(newAccount.getRole());
            accountRepository.save(existingAccount);
        } else {
            // INSERT: Account mới
            accountRepository.save(newAccount);

            // Cộng 1 vào tổng và role tương ứng
            stats.setTotalAccounts(stats.getTotalAccounts() + 1);
            incrementRoleCount(stats, newAccount.getRole());

            stats.setLastUpdated(LocalDateTime.now());
            accountStatsRepository.save(stats);

            System.out.println("New account created with role: " + newAccount.getRole());
        }

        System.out.println("Account Stats - Total: " + stats.getTotalAccounts()
                + ", Students: " + stats.getTotalStudents()
                + ", Teachers: " + stats.getTotalTeachers()
                + ", Admins: " + stats.getTotalAdmins());
    }

    @Transactional
    public void deleteAccountEvent(Long accountId) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);

        if (accountOpt.isPresent()) {
            Account account = accountOpt.get();
            AccountStats stats = getCurrentAccountStats();

            // Trừ 1 từ tổng và role tương ứng
            stats.setTotalAccounts(Math.max(0, stats.getTotalAccounts() - 1));
            decrementRoleCount(stats, account.getRole());

            stats.setLastUpdated(LocalDateTime.now());
            accountStatsRepository.save(stats);

            // Xóa account
            accountRepository.deleteById(accountId);

            System.out.println("Account deleted with role: " + account.getRole());
            System.out.println("Account Stats - Total: " + stats.getTotalAccounts()
                    + ", Students: " + stats.getTotalStudents()
                    + ", Teachers: " + stats.getTotalTeachers()
                    + ", Admins: " + stats.getTotalAdmins());
        }
    }

    private void incrementRoleCount(AccountStats stats, Role role) {
        switch (role) {
            case STUDENT:
                stats.setTotalStudents(stats.getTotalStudents() + 1);
                break;
            case TEACHER:
                stats.setTotalTeachers(stats.getTotalTeachers() + 1);
                break;
            case ADMIN:
                stats.setTotalAdmins(stats.getTotalAdmins() + 1);
                break;
        }
    }

    private void decrementRoleCount(AccountStats stats, Role role) {
        switch (role) {
            case STUDENT:
                stats.setTotalStudents(Math.max(0, stats.getTotalStudents() - 1));
                break;
            case TEACHER:
                stats.setTotalTeachers(Math.max(0, stats.getTotalTeachers() - 1));
                break;
            case ADMIN:
                stats.setTotalAdmins(Math.max(0, stats.getTotalAdmins() - 1));
                break;
        }
    }

    public AccountStats getCurrentAccountStats() {
        List<AccountStats> statsList = accountStatsRepository.findAll();
        if (statsList.isEmpty()) {
            AccountStats newStats = new AccountStats();
            newStats.setTotalAccounts(0);
            newStats.setTotalStudents(0);
            newStats.setTotalTeachers(0);
            newStats.setTotalAdmins(0);
            newStats.setLastUpdated(LocalDateTime.now());
            return accountStatsRepository.save(newStats);
        }
        return statsList.get(0);
    }

    // ============ DOCUMENT METHODS ============

    @Transactional
    public void saveDocumentEvent(Document newDocument) {
        Optional<Document> existingDocumentOpt = documentRepository.findById(newDocument.getId());
        DocumentStats stats = getCurrentDocumentStats();

        if (existingDocumentOpt.isPresent()) {
            // UPDATE: Kiểm tra xem format có thay đổi không
            Document existingDocument = existingDocumentOpt.get();
            DocumentFormat oldFormat = existingDocument.getFormat();
            DocumentFormat newFormat = newDocument.getFormat();

            if (oldFormat != newFormat) {
                // Trừ 1 từ format cũ
                decrementFormatCount(stats, oldFormat);
                // Cộng 1 vào format mới
                incrementFormatCount(stats, newFormat);

                stats.setLastUpdated(LocalDateTime.now());
                documentStatsRepository.save(stats);

                System.out.println("Document format changed from " + oldFormat + " to " + newFormat);
            }

            // Cập nhật document
            existingDocument.setFormat(newDocument.getFormat());
            documentRepository.save(existingDocument);
        } else {
            // INSERT: Document mới
            documentRepository.save(newDocument);

            // Cộng 1 vào tổng và format tương ứng
            stats.setTotalDocuments(stats.getTotalDocuments() + 1);
            incrementFormatCount(stats, newDocument.getFormat());

            stats.setLastUpdated(LocalDateTime.now());
            documentStatsRepository.save(stats);

            System.out.println("New document created with format: " + newDocument.getFormat());
        }

        System.out.println("Document Stats - Total: " + stats.getTotalDocuments()
                + ", PDF: " + stats.getTotalPdf()
                + ", Video: " + stats.getTotalVideo());
    }

    @Transactional
    public void deleteDocumentEvent(Long documentId) {
        Optional<Document> documentOpt = documentRepository.findById(documentId);

        if (documentOpt.isPresent()) {
            Document document = documentOpt.get();
            DocumentStats stats = getCurrentDocumentStats();

            // Trừ 1 từ tổng và format tương ứng
            stats.setTotalDocuments(Math.max(0, stats.getTotalDocuments() - 1));
            decrementFormatCount(stats, document.getFormat());

            stats.setLastUpdated(LocalDateTime.now());
            documentStatsRepository.save(stats);

            // Xóa document
            documentRepository.deleteById(documentId);

            System.out.println("Document deleted with format: " + document.getFormat());
            System.out.println("Document Stats - Total: " + stats.getTotalDocuments()
                    + ", PDF: " + stats.getTotalPdf()
                    + ", Video: " + stats.getTotalVideo());
        }
    }

    private void incrementFormatCount(DocumentStats stats, DocumentFormat format) {
        switch (format) {
            case PDF:
                stats.setTotalPdf(stats.getTotalPdf() + 1);
                break;
            case VIDEO:
                stats.setTotalVideo(stats.getTotalVideo() + 1);
                break;
        }
    }

    private void decrementFormatCount(DocumentStats stats, DocumentFormat format) {
        switch (format) {
            case PDF:
                stats.setTotalPdf(Math.max(0, stats.getTotalPdf() - 1));
                break;
            case VIDEO:
                stats.setTotalVideo(Math.max(0, stats.getTotalVideo() - 1));
                break;
        }
    }

    public DocumentStats getCurrentDocumentStats() {
        List<DocumentStats> statsList = documentStatsRepository.findAll();
        if (statsList.isEmpty()) {
            DocumentStats newStats = new DocumentStats();
            newStats.setTotalDocuments(0);
            newStats.setTotalPdf(0);
            newStats.setTotalVideo(0);
            newStats.setLastUpdated(LocalDateTime.now());
            return documentStatsRepository.save(newStats);
        }
        return statsList.get(0);
    }
}