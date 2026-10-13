package com.collegecopilot.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminDocumentDto {

    private String id;
    private String title;
    private String fileName;
    private Integer fileSize;
    private String uploadedBy;
    private Integer pageCount;
    private Integer chunkCount;
    private String createdAt;
    private String embeddingStatus;
    private String embeddingModel;
    private String provider = "Google Gemini";

    public AdminDocumentDto() {
    }

    public AdminDocumentDto(String id, String title, String fileName, Integer fileSize,
                            String uploadedBy, Integer pageCount, Integer chunkCount,
                            String createdAt, String embeddingStatus, String embeddingModel,
                            String provider) {
        this.id = id;
        this.title = title;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.pageCount = pageCount != null ? pageCount : 1;
        this.chunkCount = chunkCount != null ? chunkCount : 0;
        this.createdAt = createdAt;
        this.embeddingStatus = embeddingStatus;
        this.embeddingModel = embeddingModel != null ? embeddingModel : "gemini-embedding-2";
        this.provider = provider != null ? provider : "Google Gemini";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Integer getFileSize() { return fileSize; }
    public void setFileSize(Integer fileSize) { this.fileSize = fileSize; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getEmbeddingStatus() { return embeddingStatus; }
    public void setEmbeddingStatus(String embeddingStatus) { this.embeddingStatus = embeddingStatus; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
