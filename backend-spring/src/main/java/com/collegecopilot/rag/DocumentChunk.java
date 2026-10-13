package com.collegecopilot.rag;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "document_id", length = 36, nullable = false)
    private String documentId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "page_number")
    private Integer pageNumber = 1;

    @Column(name = "vector_embedding", columnDefinition = "JSON")
    private String vectorEmbedding;

    @Column(name = "embedding_model", length = 64)
    private String embeddingModel = "gemini-embedding-2";

    @Column(name = "embedding_dimension")
    private Integer embeddingDimension = 768;

    @Column(name = "embedding_version", length = 32)
    private String embeddingVersion = "v1";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public DocumentChunk() {
    }

    public DocumentChunk(String id, String documentId, Integer chunkIndex, String content, Integer pageNumber,
                         String vectorEmbedding, String embeddingModel, Integer embeddingDimension, String embeddingVersion) {
        this.id = id;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex != null ? chunkIndex : 0;
        this.content = content;
        this.pageNumber = pageNumber != null ? pageNumber : 1;
        this.vectorEmbedding = vectorEmbedding;
        this.embeddingModel = embeddingModel != null ? embeddingModel : "gemini-embedding-2";
        this.embeddingDimension = embeddingDimension != null ? embeddingDimension : 768;
        this.embeddingVersion = embeddingVersion != null ? embeddingVersion : "v1";
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPageNumber() {
        return pageNumber != null ? pageNumber : 1;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getVectorEmbedding() {
        return vectorEmbedding;
    }

    public void setVectorEmbedding(String vectorEmbedding) {
        this.vectorEmbedding = vectorEmbedding;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(Integer embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public String getEmbeddingVersion() {
        return embeddingVersion;
    }

    public void setEmbeddingVersion(String embeddingVersion) {
        this.embeddingVersion = embeddingVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt != null ? createdAt : LocalDateTime.now();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
