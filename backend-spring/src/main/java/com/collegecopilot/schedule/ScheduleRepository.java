package com.collegecopilot.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<ClassSchedule, String> {
    List<ClassSchedule> findByStudentIdOrderByCreatedAtAsc(String studentId);
}
