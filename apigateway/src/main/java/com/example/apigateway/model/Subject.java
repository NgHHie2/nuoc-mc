package com.example.apigateway.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Data;


@Data
public class Subject{
    private Integer id;

    private String title;
    private String code;
    private String description;

    @JsonProperty(access = Access.WRITE_ONLY)
    private List<Participation> participations;

}

