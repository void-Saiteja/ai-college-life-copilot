package com.collegecopilot.career;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, String> {
    List<InterviewQuestion> findByStudentIdOrderByCreatedAtDesc(String studentId);
    Optional<InterviewQuestion> findByIdAndStudentId(String id, String studentId);
}
