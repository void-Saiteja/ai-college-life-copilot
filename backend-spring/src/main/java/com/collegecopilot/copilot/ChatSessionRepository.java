package com.collegecopilot.copilot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    List<ChatSession> findByStudentIdOrderByCreatedAtDesc(String studentId);
    Optional<ChatSession> findByIdAndStudentId(String id, String studentId);
}
