package com.collegecopilot.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentDto {

    private String id;
    private String title;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_size")
    private Integer fileSize;

    @JsonProperty("uploaded_by")
    private String uploadedBy;

    @JsonProperty("chunk_count")
    private Integer chunkCount;

    @JsonProperty("created_at")
    private String createdAt;

    public DocumentDto() {
    }

    public DocumentDto(String id, String title, String fileName, Integer fileSize, String uploadedBy, Integer chunkCount, String createdAt) {
        this.id = id;
        this.title = title;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.chunkCount = chunkCount;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
