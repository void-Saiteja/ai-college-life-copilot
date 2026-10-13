package com.collegecopilot.career;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, String> {
    List<Resume> findByStudentIdOrderByCreatedAtDesc(String studentId);
    Optional<Resume> findByIdAndStudentId(String id, String studentId);
}
