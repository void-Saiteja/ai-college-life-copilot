package com.collegecopilot.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, String> {

    List<Expense> findByStudentIdOrderByExpenseDateDescCreatedAtDesc(String studentId);

    Optional<Expense> findByIdAndStudentId(String id, String studentId);
}
