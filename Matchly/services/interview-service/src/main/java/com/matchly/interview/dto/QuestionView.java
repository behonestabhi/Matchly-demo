package com.matchly.interview.dto;

import com.matchly.interview.domain.Question;

/** A single question in the GET response. */
public record QuestionView(
        String category,
        String question,
        String suggestedAnswer,
        String difficulty) {

    public static QuestionView from(Question q) {
        return new QuestionView(
                q.getCategory() == null ? null : q.getCategory().name(),
                q.getQuestion(),
                q.getSuggestedAnswer(),
                q.getDifficulty());
    }
}
