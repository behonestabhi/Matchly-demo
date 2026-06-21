package com.matchly.candidate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.matchly.candidate.domain.Candidate;
import com.matchly.candidate.dto.UpdateProfileRequest;
import com.matchly.candidate.exception.NotFoundException;
import com.matchly.candidate.repository.CandidateRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Profile read/update operations for candidates. */
@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    /**
     * Return the candidate profile for the given user, creating an empty one on
     * first access (profiles are provisioned lazily when the user first hits the
     * candidate-service, since accounts live in auth-service).
     */
    @Transactional
    public Candidate getOrCreateByUserId(UUID userId) {
        return candidateRepository.findByUserId(userId)
                .orElseGet(() -> candidateRepository.save(new Candidate(UUID.randomUUID(), userId)));
    }

    @Transactional(readOnly = true)
    public Candidate getById(UUID candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate not found: " + candidateId));
    }

    @Transactional(readOnly = true)
    public Candidate requireByUserId(UUID userId) {
        return candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No candidate profile for the current user"));
    }

    @Transactional
    public Candidate updateProfile(UUID userId, UpdateProfileRequest request) {
        Candidate candidate = getOrCreateByUserId(userId);
        if (request.headline() != null) {
            candidate.setHeadline(request.headline());
        }
        if (request.location() != null) {
            candidate.setLocation(request.location());
        }
        if (request.phone() != null) {
            candidate.setPhone(request.phone());
        }
        if (request.links() != null) {
            JsonNode links = request.links();
            candidate.setLinks(links.isNull() ? null : links.toString());
        }
        return candidateRepository.save(candidate);
    }
}
