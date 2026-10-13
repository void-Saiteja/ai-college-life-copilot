package com.collegecopilot.health;

public class HealthResponse {

    private boolean success;
    private String status;
    private String message;
    private String timestamp;
    private String environment;
    private String version;

    public HealthResponse() {
    }

    public HealthResponse(boolean success, String status, String message, String timestamp, String environment, String version) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.environment = environment;
        this.version = version;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
