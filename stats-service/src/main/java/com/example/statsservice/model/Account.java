package com.example.statsservice.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Account {
    private Integer id;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private LocalDateTime birthDay;
    private String phoneNumber;
    private String email;
}