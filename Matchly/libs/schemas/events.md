# Event Catalog

All events use the [common envelope](event-envelope.schema.json). `payload`
shapes per event type:

| Topic | eventType | payload |
|---|---|---|
| `candidate.events` | `ResumeUploaded` | `{ candidateId, resumeId, s3Key, fileName, mimeType }` |
| `candidate.events` | `ProfileUpdated` | `{ candidateId, fields[] }` |
| `resume.parsed` | `ResumeParsed` | `{ candidateId, resumeId, skills[], totalExpYrs, embeddingRef }` |
| `job.events` | `JobPosted` | `{ jobId, recruiterId, requiredSkills[], minExpYrs, maxExpYrs }` |
| `job.events` | `JobClosed` | `{ jobId }` |
| `job.embedded` | `JobEmbedded` | `{ jobId, embeddingRef }` |
| `application.events` | `ApplicationSubmitted` | `{ applicationId, candidateId, jobId }` |
| `application.events` | `StageChanged` | `{ applicationId, fromStage, toStage, actorId }` |
| `matching.events` | `MatchScored` | `{ candidateId, jobId, finalScore, modelVersion }` |
| `matching.events` | `SkillGapComputed` | `{ candidateId, jobId, missingSkills[] }` |
| `interview.events` | `QuestionsGenerated` | `{ candidateId, jobId, questionSetId }` |

Partition keys and consumer responsibilities are documented in
[`../../docs/DATA_MODELS.md`](../../docs/DATA_MODELS.md) §9.
