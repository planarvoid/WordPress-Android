package com.soundcloud.gradle.violations;

class StaticAnalysisFoundViolationException extends Exception {
    StaticAnalysisFoundViolationException(String message) {
        super(message);
    }
}
