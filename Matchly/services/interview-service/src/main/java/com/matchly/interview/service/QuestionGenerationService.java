package com.matchly.interview.service;

import com.matchly.interview.client.ai.AiInterviewRequest;
import com.matchly.interview.client.ai.AiInterviewResponse;
import com.matchly.interview.client.ai.AiServiceClient;
import com.matchly.interview.client.lookup.LookupClient;
import com.matchly.interview.dto.CreateInterviewRequest;
import com.matchly.interview.events.InterviewEventProducer;
import com.matchly.interview.events.QuestionsGeneratedPayload;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Runs the heavy LLM generation off the request thread.
 *
 * <p>Flow: (optionally) enrich resume/JD/skills from candidate/job services →
 * call the AI service → persist questions + set status READY (or FAILED on
 * error) → publish {@code QuestionsGenerated}. Persistence is delegated to
 * {@link QuestionSetPersistenceService} so the {@code @Transactional} boundary
 * is honoured by the Spring proxy (this method runs on an async thread).
 */
@Service
public class QuestionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationService.class);

    private final AiServiceClient aiServiceClient;
    private final LookupClient lookupClient;
    private final InterviewEventProducer eventProducer;
    private final QuestionSetPersistenceService persistenceService;

    public QuestionGenerationService(AiServiceClient aiServiceClient,
                                     LookupClient lookupClient,
                                     InterviewEventProducer eventProducer,
                                     QuestionSetPersistenceService persistenceService) {
        this.aiServiceClient = aiServiceClient;
        this.lookupClient = lookupClient;
        this.eventProducer = eventProducer;
        this.persistenceService = persistenceService;
    }

    @Async("interviewGenerationExecutor")
    public void generateAsync(UUID questionSetId, CreateInterviewRequest request, String correlationId) {
        try {
            AiInterviewResponse response = callAi(request);
            persistenceService.markReady(questionSetId, response);
            eventProducer.publishQuestionsGenerated(
                    new QuestionsGeneratedPayload(request.candidateId(), request.jobId(), questionSetId),
                    correlationId);
            log.info("Question set {} READY ({} questions)", questionSetId, response.questions().size());
        } catch (Exception e) {
            log.error("Question set {} generation FAILED: {}", questionSetId, e.getMessage());
            persistenceService.markFailed(questionSetId, e.getMessage());
        }
    }

    /** Build the AI request, enriching from candidate/job services when fields are missing. */
    private AiInterviewResponse callAi(CreateInterviewRequest request) {
        String resumeText = request.resumeText();
        String jobDescription = request.jobDescription();
        List<String> skills = AiServiceClient.safeSkills(request.skills());

        if (!StringUtils.hasText(resumeText) || skills.isEmpty()) {
            LookupClient.CandidateInfo candidate = lookupClient.fetchCandidate(request.candidateId());
            if (!StringUtils.hasText(resumeText)) {
                resumeText = candidate.resumeText();
            }
            if (skills.isEmpty()) {
                skills = candidate.skillsOrEmpty();
            }
        }
        if (!StringUtils.hasText(jobDescription)) {
            jobDescription = lookupClient.fetchJob(request.jobId()).description();
        }

        AiInterviewRequest aiRequest = new AiInterviewRequest(
                resumeText, jobDescription, skills, AiServiceClient.defaultCounts());
        return aiServiceClient.generate(aiRequest);
    }
}
