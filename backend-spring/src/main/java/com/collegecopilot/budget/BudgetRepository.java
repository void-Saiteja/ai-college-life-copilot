package com.collegecopilot.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, String> {

    Optional<Budget> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);
}
