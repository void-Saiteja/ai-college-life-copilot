package com.collegecopilot.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String> {
    List<Attendance> findByStudentId(String studentId);
    java.util.Optional<Attendance> findBySubjectIdAndStudentId(String subjectId, String studentId);
}
