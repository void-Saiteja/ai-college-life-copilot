package com.collegecopilot.career;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewPracticeRepository extends JpaRepository<InterviewPractice, String> {
    List<InterviewPractice> findByStudentIdOrderByCreatedAtDesc(String studentId);
    Optional<InterviewPractice> findByIdAndStudentId(String id, String studentId);
}
