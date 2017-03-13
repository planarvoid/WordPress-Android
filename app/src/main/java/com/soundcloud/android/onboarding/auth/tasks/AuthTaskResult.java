package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.onboarding.auth.response.AuthResponse;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;

public class AuthTaskResult {
    private final TaskResultKind kind;
    private final AuthResponse authResponse;
    private final SignupVia signupVia;
    private final Exception exception;
    private final Bundle loginBundle;
    private final String errorMessage;

    private AuthTaskResult(AuthResponse authResponse, SignupVia signupVia) {
        this(TaskResultKind.SUCCESS, authResponse, signupVia, null, null, null);
    }

    private AuthTaskResult(Exception exception) {
        this(TaskResultKind.FAILURE, null, null, exception, null, null);
    }

    private AuthTaskResult(TaskResultKind kind, String errorMessage, ApiRequestException exception) {
        this(kind, null, null, exception, null, errorMessage);
    }

    private AuthTaskResult(TaskResultKind kind, Exception exception) {
        this(kind, null, null, exception, null, null);
    }

    private AuthTaskResult(TaskResultKind kind) {
        this(kind, null, null, null, null, null);
    }

    AuthTaskResult(@NotNull TaskResultKind kind,
                           AuthResponse authResponse,
                           SignupVia signupVia,
                           Exception exception,
                           Bundle loginBundle,
                           String errorMessage) {
        this.kind = kind;
        this.authResponse = authResponse;
        this.signupVia = signupVia;
        this.exception = exception;
        this.loginBundle = loginBundle;
        this.errorMessage = errorMessage;
    }

    public static AuthTaskResult success(AuthResponse authResponse, SignupVia signupVia) {
        return new AuthTaskResult(authResponse, signupVia);
    }

    public static AuthTaskResult failure(Exception exception) {
        return new AuthTaskResult(exception);
    }

    public static AuthTaskResult failure(String errorMessage) {
        return failure(new AuthTaskException(errorMessage));
    }

    public static AuthTaskResult failure(String errorMessge, ApiRequestException exception) {
        return new AuthTaskResult(TaskResultKind.FAILURE, errorMessge, exception);
    }

    public static AuthTaskResult emailTaken(ApiRequestException exception) {
        return new AuthTaskResult(TaskResultKind.EMAIL_TAKEN, exception);
    }

    public static AuthTaskResult spam(ApiRequestException exception) {
        return new AuthTaskResult(TaskResultKind.SPAM, exception);
    }

    public static AuthTaskResult denied(ApiRequestException exception) {
        return new AuthTaskResult(TaskResultKind.DENIED, exception);
    }

    public static AuthTaskResult emailInvalid(ApiRequestException exception) {
        return new AuthTaskResult(TaskResultKind.EMAIL_INVALID, exception);
    }

    public static AuthTaskResult unauthorized(ApiRequestException exception) {
        return new AuthTaskResult(TaskResultKind.UNAUTHORIZED, exception);
    }

    public static AuthTaskResult serverError(ApiRequestException exception) {
        return new AuthTaskResult(TaskResultKind.SERVER_ERROR, exception);
    }

    public static AuthTaskResult networkError(Exception exception) {
        return new AuthTaskResult(TaskResultKind.NETWORK_ERROR, exception);
    }

    public static AuthTaskResult validationError(String errorMessage, ApiRequestException exception) {
        return new AuthTaskResult(TaskResultKind.VALIDATION_ERROR, errorMessage, exception);
    }

    public static AuthTaskResult deviceConflict(Bundle loginBundle) {
        return new AuthTaskResult(TaskResultKind.DEVICE_CONFLICT, null, null, null, loginBundle, null);
    }

    public static AuthTaskResult incorrectCredentials(ApiRequestException exception) {
        return new AuthTaskResult(TaskResultKind.UNAUTHORIZED, exception);
    }

    public static AuthTaskResult deviceBlock() {
        return new AuthTaskResult(TaskResultKind.DEVICE_BLOCK);
    }

    public boolean wasUnexpectedError() {
        return kind.isUnexpectedError();
    }

    public boolean wasSuccess() {
        return kind == TaskResultKind.SUCCESS;
    }

    public boolean wasAgeRestricted() {
        return false;
    }

    public boolean wasFailure() {
        return kind == TaskResultKind.FAILURE;
    }

    public boolean wasEmailTaken() {
        return kind == TaskResultKind.EMAIL_TAKEN;
    }

    public boolean wasSpam() {
        return kind == TaskResultKind.SPAM;
    }

    public boolean wasDenied() {
        return kind == TaskResultKind.DENIED;
    }

    public boolean wasUnauthorized() {
        return kind == TaskResultKind.UNAUTHORIZED;
    }

    public boolean wasServerError() {
        return kind == TaskResultKind.SERVER_ERROR;
    }

    public boolean wasNetworkError() {
        return kind == TaskResultKind.NETWORK_ERROR;
    }

    public boolean wasEmailInvalid() {
        return kind == TaskResultKind.EMAIL_INVALID;
    }

    public boolean wasSignUpFailedToLogin() {
        return kind == TaskResultKind.FLAKY_SIGNUP_ERROR;
    }

    public boolean wasDeviceConflict() {
        return kind == TaskResultKind.DEVICE_CONFLICT;
    }

    public boolean wasDeviceBlock() {
        return kind == TaskResultKind.DEVICE_BLOCK;
    }

    public boolean wasValidationError() {
        return kind == TaskResultKind.VALIDATION_ERROR;
    }

    public AuthResponse getAuthResponse() {
        return authResponse;
    }

    public SignupVia getSignupVia() {
        return signupVia;
    }

    public Exception getException() {
        return exception;
    }

    public Bundle getLoginBundle() {
        return loginBundle;
    }

    public TaskResultKind getKind() {
        return kind;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return String.format(
                "Auth task result with\n\tkind: %s\n\tuser present: %b\n\tvia: %s\n\texception: %s\n\tbundle present: %b\n\tserver error: %s",
                kind,
                authResponse != null,
                signupVia,
                exception,
                loginBundle != null,
                errorMessage
        );
    }
}
