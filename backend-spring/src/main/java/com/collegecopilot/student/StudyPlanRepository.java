package com.collegecopilot.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, String> {
    Optional<StudyPlan> findFirstByStudentIdAndStatus(String studentId, String status);
    List<StudyPlan> findByStudentId(String studentId);
}
