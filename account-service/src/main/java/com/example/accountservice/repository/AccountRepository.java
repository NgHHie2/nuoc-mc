package com.example.accountservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.accountservice.model.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer> {

    // Basic queries with visible filter
    Page<Account> findByVisible(Integer visible, Pageable pageable);

    Optional<Account> findByIdAndVisible(Integer id, Integer visible);

    List<Account> findAllByIdInAndVisible(List<Integer> ids, Integer visible);

    // Username queries
    Optional<Account> findByUsername(String username);

    Optional<Account> findByUsernameAndVisible(String username, Integer visible);

    boolean existsByUsernameAndVisible(String username, Integer visible);

    Page<Account> findByUsernameContainingIgnoreCaseAndVisible(String username, Integer visible, Pageable pageable);

    // Email queries
    Optional<Account> findByEmailAndVisible(String email, Integer visible);

    boolean existsByEmailAndVisible(String email, Integer visible);

    Page<Account> findByEmailContainingIgnoreCaseAndVisible(String email, Integer visible, Pageable pageable);

    // CCCD queries
    Optional<Account> findByCccdAndVisible(String cccd, Integer visible);

    boolean existsByCccdAndVisible(String cccd, Integer visible);

    Page<Account> findByCccdContainingAndVisible(String cccd, Integer visible, Pageable pageable);

    // Search queries
    Page<Account> findByFirstNameContainingIgnoreCaseAndVisible(String firstName, Integer visible, Pageable pageable);

    Page<Account> findByLastNameContainingIgnoreCaseAndVisible(String lastName, Integer visible, Pageable pageable);

    Page<Account> findByPhoneNumberContainingAndVisible(String phoneNumber, Integer visible, Pageable pageable);

    // Global search query
    @Query("SELECT a FROM Account a WHERE a.visible = :visible AND " +
            "(LOWER(a.firstName) LIKE LOWER(:keyword) OR " +
            "LOWER(a.lastName) LIKE LOWER(:keyword) OR " +
            "LOWER(a.username) LIKE LOWER(:keyword) OR " +
            "LOWER(a.email) LIKE LOWER(:keyword) OR " +
            "a.cccd LIKE :keyword OR " +
            "a.phoneNumber LIKE :keyword)")
    Page<Account> searchByKeywordAndVisible(@Param("keyword") String keyword, @Param("visible") Integer visible,
            Pageable pageable);
}