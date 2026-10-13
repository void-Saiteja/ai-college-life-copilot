package com.collegecopilot.note;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, String> {

    List<Note> findByStudentIdOrderByUpdatedAtDesc(String studentId);

    @Query("SELECT n FROM Note n WHERE n.studentId = :studentId OR (:isStd1 = true AND (n.studentId IS NULL OR n.studentId = 'std-1')) ORDER BY n.updatedAt DESC")
    List<Note> findByStudentIdWithLegacy(@Param("studentId") String studentId, @Param("isStd1") boolean isStd1);
}
