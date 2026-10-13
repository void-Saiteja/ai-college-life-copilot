package com.collegecopilot.career;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CareerProfileRepository extends JpaRepository<CareerProfile, String> {

    Optional<CareerProfile> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);
}
