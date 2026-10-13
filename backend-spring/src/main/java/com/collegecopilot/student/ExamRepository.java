package com.collegecopilot.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, String> {
    List<Exam> findByStudentIdOrderByExamDateAsc(String studentId);
    java.util.Optional<Exam> findByIdAndStudentId(String id, String studentId);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Exam e WHERE e.studentId = :studentId OR (:studentId = 'std-1' AND e.studentId IS NULL) ORDER BY e.examDate ASC")
    List<Exam> findByStudentIdOrLegacyOrderByExamDateAsc(@org.springframework.data.repository.query.Param("studentId") String studentId);
}
