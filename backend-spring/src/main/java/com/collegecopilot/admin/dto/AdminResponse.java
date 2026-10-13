package com.collegecopilot.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminResponse<T> {

    private boolean success = true;
    private T data;
    private Integer count;
    private String error;

    public AdminResponse() {
    }

    public AdminResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public AdminResponse(boolean success, T data, Integer count) {
        this.success = success;
        this.data = data;
        this.count = count;
    }

    public static <T> AdminResponse<T> ok(T data) {
        return new AdminResponse<>(true, data);
    }

    public static <T> AdminResponse<T> ok(T data, int count) {
        return new AdminResponse<>(true, data, count);
    }

    public static <T> AdminResponse<T> error(String error) {
        AdminResponse<T> res = new AdminResponse<>();
        res.setSuccess(false);
        res.setError(error);
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
