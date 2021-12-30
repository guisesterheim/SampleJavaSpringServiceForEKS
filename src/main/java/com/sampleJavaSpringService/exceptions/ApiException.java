package com.sampleJavaSpringService.exceptions;

import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

public class ApiException {

    private HttpStatus status;
    private String message;
    private List<String> stackTrace;

    public ApiException(HttpStatus status, String message, List<String> stackTrace) {
        super();
        this.status = status;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public ApiException(HttpStatus status, String message, String error) {
        super();
        this.status = status;
        this.message = message;
        stackTrace = Arrays.asList(error);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(List<String> stackTrace) {
        this.stackTrace = stackTrace;
    }
}