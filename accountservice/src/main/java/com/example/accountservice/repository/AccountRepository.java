package com.example.accountservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.accountservice.model.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, Integer>{

}
