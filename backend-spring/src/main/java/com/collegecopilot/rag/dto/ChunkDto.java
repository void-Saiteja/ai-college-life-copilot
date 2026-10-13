package com.collegecopilot.rag.dto;

public class ChunkDto {

    private String id;
    private String documentId;
    private String documentTitle;
    private int pageNumber;
    private int chunkIndex;
    private String content;
    private String vectorEmbedding;
    private String embeddingModel;
    private int embeddingDimension;
    private String embeddingVersion;
    private double score;

    public ChunkDto() {
    }

    public ChunkDto(String id, String documentId, String documentTitle, int pageNumber, int chunkIndex,
                    String content, String vectorEmbedding, String embeddingModel, int embeddingDimension,
                    String embeddingVersion, double score) {
        this.id = id;
        this.documentId = documentId;
        this.documentTitle = documentTitle;
        this.pageNumber = pageNumber;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.vectorEmbedding = vectorEmbedding;
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
        this.embeddingVersion = embeddingVersion;
        this.score = score;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }

    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getVectorEmbedding() { return vectorEmbedding; }
    public void setVectorEmbedding(String vectorEmbedding) { this.vectorEmbedding = vectorEmbedding; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public int getEmbeddingDimension() { return embeddingDimension; }
    public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }

    public String getEmbeddingVersion() { return embeddingVersion; }
    public void setEmbeddingVersion(String embeddingVersion) { this.embeddingVersion = embeddingVersion; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
