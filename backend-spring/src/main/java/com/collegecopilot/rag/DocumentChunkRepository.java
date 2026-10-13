package com.collegecopilot.rag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(String documentId);

    @Modifying
    @Query("DELETE FROM DocumentChunk c WHERE c.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") String documentId);
}
