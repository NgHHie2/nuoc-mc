package com.example.apigateway.model;

import lombok.Data;

@Data
public class Participation{
    private Integer id;
    private Account account;
    private Subject subject;
}
