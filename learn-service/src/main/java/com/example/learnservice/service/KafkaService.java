package com.example.learnservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.learnservice.dto.AccountDTO;
import com.example.learnservice.enums.Role;
import com.example.learnservice.repository.SemesterAccountRepository;
import com.example.learnservice.repository.SemesterTeacherRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KafkaService {

    @Autowired
    private SemesterAccountRepository semesterAccountRepository;

    @Autowired
    private SemesterTeacherRepository semesterTeacherRepository;

    /**
     * Xử lý khi account bị xóa
     * Xóa tất cả references trong SemesterAccount và SemesterTeacher
     */
    @Transactional
    public void handleAccountDeleted(Long accountId) {
        log.info("Processing account deletion for accountId: {}", accountId);

        // Xóa khỏi SemesterAccount
        int deletedFromAccounts = semesterAccountRepository.deleteByAccountId(accountId);
        log.info("Deleted {} records from SemesterAccount for accountId: {}",
                deletedFromAccounts, accountId);

        // Xóa khỏi SemesterTeacher
        int deletedFromTeachers = semesterTeacherRepository.deleteByTeacherId(accountId);
        log.info("Deleted {} records from SemesterTeacher for accountId: {}",
                deletedFromTeachers, accountId);
    }

    /**
     * Xử lý khi account được update (thay đổi role)
     * - Nếu role mới là TEACHER: xóa khỏi SemesterAccount
     * - Nếu role mới là STUDENT: xóa khỏi SemesterTeacher
     * - Nếu role mới là ADMIN: xóa khỏi cả 2 bảng
     */
    @Transactional
    public void handleAccountUpdated(AccountDTO account) {
        log.info("Processing account update for accountId: {}, new role: {}",
                account.getId(), account.getRole());

        if (account.getRole() == null) {
            log.warn("Account role is null for accountId: {}", account.getId());
            return;
        }

        switch (account.getRole()) {
            case TEACHER:
                // Nếu role mới là TEACHER, xóa khỏi SemesterAccount (vì teacher không thể là
                // student)
                int deletedAccounts = semesterAccountRepository.deleteByAccountId(account.getId());
                log.info("Role changed to TEACHER. Deleted {} records from SemesterAccount for accountId: {}",
                        deletedAccounts, account.getId());
                break;

            case STUDENT:
                // Nếu role mới là STUDENT, xóa khỏi SemesterTeacher (vì student không thể là
                // teacher)
                int deletedTeachers = semesterTeacherRepository.deleteByTeacherId(account.getId());
                log.info("Role changed to STUDENT. Deleted {} records from SemesterTeacher for accountId: {}",
                        deletedTeachers, account.getId());
                break;

            case ADMIN:
                // Nếu role mới là ADMIN, xóa khỏi cả 2 bảng
                int deletedFromAccounts = semesterAccountRepository.deleteByAccountId(account.getId());
                int deletedFromTeachers = semesterTeacherRepository.deleteByTeacherId(account.getId());
                log.info(
                        "Role changed to ADMIN. Deleted {} records from SemesterAccount and {} records from SemesterTeacher for accountId: {}",
                        deletedFromAccounts, deletedFromTeachers, account.getId());
                break;

            default:
                log.warn("Unknown role: {} for accountId: {}", account.getRole(), account.getId());
                break;
        }
    }
}
