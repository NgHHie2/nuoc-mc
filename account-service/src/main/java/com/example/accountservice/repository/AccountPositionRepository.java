package com.example.accountservice.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.accountservice.model.AccountPosition;
import com.example.accountservice.model.Account;
import com.example.accountservice.model.Position;

@Repository
public interface AccountPositionRepository extends JpaRepository<AccountPosition, Long> {

    List<AccountPosition> findByAccount(Account account);

    List<AccountPosition> findByPosition(Position position);

    boolean existsByAccountAndPosition(Account account, Position position);

    boolean deleteByAccountAndPosition(Account account, Position position);
}