package com.collegecopilot.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, String> {
    List<Assignment> findByStudentIdAndStatusNotOrderByDueDateAsc(String studentId, String status);
    List<Assignment> findByStudentId(String studentId);
    List<Assignment> findByStudentIdOrderByDueDateAsc(String studentId);
    java.util.Optional<Assignment> findByIdAndStudentId(String id, String studentId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Assignment a WHERE a.studentId = :studentId OR (:studentId = 'std-1' AND a.studentId IS NULL) ORDER BY a.dueDate ASC")
    List<Assignment> findByStudentIdOrLegacyOrderByDueDateAsc(@org.springframework.data.repository.query.Param("studentId") String studentId);
}
