package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;

/**
 *Once the public API usages for authentication are gone we only use {@link AuthTaskResult}
 */
@Deprecated
public final class LegacyAuthTaskResult {

    private final TaskResultKind kind;
    private final ApiUser user;
    private final SignupVia signupVia;
    private final Exception exception;

    private final Bundle loginBundle;

    // Can be dropped once we move away from public API for signups
    @Deprecated private String serverErrorMessage;

    public static LegacyAuthTaskResult success(ApiUser user, SignupVia signupVia) {
        return new LegacyAuthTaskResult(user, signupVia);
    }

    public static LegacyAuthTaskResult failure(Exception exception) {
        return new LegacyAuthTaskResult(exception);
    }

    public static LegacyAuthTaskResult failure(ApiRequestException exception) {
        switch (exception.reason()) {
            case AUTH_ERROR:
                return LegacyAuthTaskResult.unauthorized(exception);
            case VALIDATION_ERROR:
                return LegacyAuthTaskResult.validationError(exception.errorKey(), exception);
            case NETWORK_ERROR:
                return LegacyAuthTaskResult.networkError((Exception) exception.getCause());
            case SERVER_ERROR:
                return LegacyAuthTaskResult.serverError(exception);
            default:
                return new LegacyAuthTaskResult(exception);
        }
    }

    public static LegacyAuthTaskResult failure(String errorMessage) {
        return failure(new AuthTaskException(errorMessage));
    }

    public static LegacyAuthTaskResult failure(String errorMessge, ApiRequestException exception) {
        return new LegacyAuthTaskResult(TaskResultKind.FAILURE, errorMessge, exception);
    }

    public static LegacyAuthTaskResult emailTaken(ApiRequestException exception) {
        return new LegacyAuthTaskResult(TaskResultKind.EMAIL_TAKEN, exception);
    }

    public static LegacyAuthTaskResult spam(ApiRequestException exception) {
        return new LegacyAuthTaskResult(TaskResultKind.SPAM, exception);
    }

    public static LegacyAuthTaskResult denied(ApiRequestException exception) {
        return new LegacyAuthTaskResult(TaskResultKind.DENIED, exception);
    }

    public static LegacyAuthTaskResult emailInvalid(ApiRequestException exception) {
        return new LegacyAuthTaskResult(TaskResultKind.EMAIL_INVALID, exception);
    }

    public static LegacyAuthTaskResult signUpFailedToLogin(ApiRequestException exception) {
        return new LegacyAuthTaskResult(TaskResultKind.FLAKY_SIGNUP_ERROR, exception);
    }

    public static LegacyAuthTaskResult unauthorized(ApiRequestException exception) {
        return new LegacyAuthTaskResult(TaskResultKind.UNAUTHORIZED, exception);
    }

    public static LegacyAuthTaskResult serverError(ApiRequestException exception) {
        return new LegacyAuthTaskResult(TaskResultKind.SERVER_ERROR, exception);
    }

    public static LegacyAuthTaskResult networkError(Exception exception) {
        return new LegacyAuthTaskResult(TaskResultKind.NETWORK_ERROR, exception);
    }

    public static LegacyAuthTaskResult validationError(String errorMessage, ApiRequestException exception) {
        return new LegacyAuthTaskResult(TaskResultKind.VALIDATION_ERROR, errorMessage, exception);
    }

    public static LegacyAuthTaskResult deviceConflict(Bundle loginBundle) {
        return new LegacyAuthTaskResult(TaskResultKind.DEVICE_CONFLICT, null, null, null, loginBundle, null);
    }

    public static LegacyAuthTaskResult deviceBlock() {
        return new LegacyAuthTaskResult(TaskResultKind.DEVICE_BLOCK);
    }

    public boolean wasUnexpectedError() {
        return kind.isUnexpectedError();
    }

    public static LegacyAuthTaskResult fromAuthTaskResult(AuthTaskResult result) {
        ApiUser user = result.getAuthResponse() == null ? null : result.getAuthResponse().me.getUser();
        return new LegacyAuthTaskResult(result.getKind(), user, result.getSignupVia(), result.getException(), result.getLoginBundle(), result.getErrorMessage());
    }

    private LegacyAuthTaskResult(ApiUser user, SignupVia signupVia) {
        this(TaskResultKind.SUCCESS, user, signupVia, null, null, null);
    }

    private LegacyAuthTaskResult(Exception exception) {
        this(TaskResultKind.FAILURE, null, null, exception, null, null);
    }

    private LegacyAuthTaskResult(TaskResultKind kind, String serverErrorMessage, ApiRequestException exception) {
        this(kind, null, null, exception, null, serverErrorMessage);
    }

    private LegacyAuthTaskResult(TaskResultKind kind, Exception exception) {
        this(kind, null, null, exception, null, null);
    }

    private LegacyAuthTaskResult(TaskResultKind kind) {
        this(kind, null, null, null, null, null);
    }

    private LegacyAuthTaskResult(@NotNull TaskResultKind kind, ApiUser user, SignupVia signupVia,
                                 Exception exception, Bundle loginBundle,
                                 String serverErrorMessage) {
        this.kind = kind;
        this.user = user;
        this.signupVia = signupVia;
        this.exception = exception;
        this.loginBundle = loginBundle;
        this.serverErrorMessage = serverErrorMessage;
    }

    public boolean wasSuccess() {
        return kind == TaskResultKind.SUCCESS;
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

    public ApiUser getUser() {
        return user;
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

    @Deprecated
    public String getServerErrorMessage() {
        return serverErrorMessage;
    }

    @Override
    public String toString() {
        return String.format(
                "Auth task result with\n\tkind: %s\n\tuser present: %b\n\tvia: %s\n\texception: %s\n\tbundle present: %b\n\tserver error: %s",
                kind,
                user != null,
                signupVia,
                exception,
                loginBundle != null,
                serverErrorMessage
        );
    }
}
