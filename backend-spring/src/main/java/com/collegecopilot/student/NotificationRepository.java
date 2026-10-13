package com.collegecopilot.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId OR (:studentId IS NOT NULL AND n.userId = :studentId) ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrStudentIdOrderByCreatedAtDesc(@Param("userId") String userId, @Param("studentId") String studentId);
}
