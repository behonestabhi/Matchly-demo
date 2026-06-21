package com.matchly.application.repository;

import com.matchly.application.domain.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
