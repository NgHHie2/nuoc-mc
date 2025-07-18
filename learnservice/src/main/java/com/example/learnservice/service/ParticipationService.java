package com.example.learnservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.learnservice.dto.ParticipationDTO;
import com.example.learnservice.model.Account;
import com.example.learnservice.model.Participation;
import com.example.learnservice.repository.ParticipationRepository;

@Service
public class ParticipationService {
    @Autowired
    private ParticipationRepository participationRepository;

    public List<ParticipationDTO> getParticipationByAccount(Account account) {
        List<Participation> participations = participationRepository.findByAccount(account);
        List<ParticipationDTO> dtoList = participations.stream().map(ParticipationDTO::fromEntity).toList();
        return dtoList;
    }
}
