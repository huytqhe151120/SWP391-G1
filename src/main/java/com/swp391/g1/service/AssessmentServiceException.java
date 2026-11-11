package com.swp391.g1.service;

/**
 * Business exception for the Post-Activity Assessment feature.
 * Used to convey validation and authorization failures to the controller layer.
 */
public class AssessmentServiceException extends RuntimeException {

    public AssessmentServiceException(String message) {
        super(message);
    }

    public AssessmentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
