package com.soundcloud.android.framework.helpers.mrlogga;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class FailedValidationResponse implements ValidationResponse {

    private String status;
    private String message;
    private List<ValidationResult> validationResults;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("event_validation_results")
    public void setValidationResults(List<ValidationResult> validationResults) {
        this.validationResults = validationResults;
    }

    @Override
    public boolean isSuccessful() {
        return false;
    }

    @Override
    public String toString() {
        if (message != null) {
            return "\nError message received:\n" + message;
        }

        final StringBuilder stringBuilder = new StringBuilder("\nValidation Results:\n");
        for (int i = 0; i < validationResults.size(); i++) {
            final ValidationResult result = validationResults.get(i);
            stringBuilder.append(i)
                         .append(". ")
                         .append(result)
                         .append("\n");
        }
        return stringBuilder.toString();
    }
}
