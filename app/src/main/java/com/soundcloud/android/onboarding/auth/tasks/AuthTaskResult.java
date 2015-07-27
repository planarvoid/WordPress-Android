package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;
import org.jetbrains.annotations.NotNull;

import android.os.Bundle;

import java.util.EnumSet;

public final class AuthTaskResult {

    private final Kind kind;
    private final PublicApiUser user;
    private final SignupVia signupVia;
    private final Exception exception;

    private final boolean showFacebookSuggestions;
    private final Bundle loginBundle;

    // Can be dropped once we move away from public API for signups
    @Deprecated private String serverErrorMessage;

    public static AuthTaskResult success(PublicApiUser user, SignupVia signupVia, boolean showFacebookSuggestions) {
        return new AuthTaskResult(user, signupVia, showFacebookSuggestions);
    }

    public static AuthTaskResult failure(Exception exception) {
        return new AuthTaskResult(exception);
    }

    public static AuthTaskResult failure(ApiRequestException exception) {
        switch (exception.reason()) {
            case AUTH_ERROR:
                return AuthTaskResult.unauthorized(exception);
            case VALIDATION_ERROR:
                return AuthTaskResult.validationError(exception.errorKey(), exception);
            case NETWORK_ERROR:
                return AuthTaskResult.networkError((Exception) exception.getCause());
            case SERVER_ERROR:
                return AuthTaskResult.serverError(exception);
            default:
                return new AuthTaskResult(exception);
        }
    }

    public static AuthTaskResult failure(String errorMessage) {
        return failure(new AuthTaskException(errorMessage));
    }

    public static AuthTaskResult failure(String errorMessge, ApiRequestException exception) {
        return new AuthTaskResult(Kind.FAILURE, errorMessge, exception);
    }

    public static AuthTaskResult emailTaken(ApiRequestException exception) {
        return new AuthTaskResult(Kind.EMAIL_TAKEN, exception);
    }

    public static AuthTaskResult spam(ApiRequestException exception) {
        return new AuthTaskResult(Kind.SPAM, exception);
    }

    public static AuthTaskResult denied(ApiRequestException exception) {
        return new AuthTaskResult(Kind.DENIED, exception);
    }

    public static AuthTaskResult emailInvalid(ApiRequestException exception) {
        return new AuthTaskResult(Kind.EMAIL_INVALID, exception);
    }

    public static AuthTaskResult signUpFailedToLogin(ApiRequestException exception) {
        return new AuthTaskResult(Kind.FLAKY_SIGNUP_ERROR, exception);
    }

    public static AuthTaskResult unauthorized(ApiRequestException exception) {
        return new AuthTaskResult(Kind.UNAUTHORIZED, exception);
    }

    public static AuthTaskResult serverError(ApiRequestException exception) {
        return new AuthTaskResult(Kind.SERVER_ERROR, exception);
    }

    public static AuthTaskResult networkError(Exception exception) {
        return new AuthTaskResult(Kind.NETWORK_ERROR, exception);
    }

    public static AuthTaskResult validationError(String errorMessage, ApiRequestException exception) {
        return new AuthTaskResult(Kind.VALIDATION_ERROR, errorMessage, exception);
    }

    public static AuthTaskResult deviceConflict(Bundle loginBundle) {
        return new AuthTaskResult(Kind.DEVICE_CONFLICT, null, null, null, false, loginBundle, null);
    }

    public boolean wasUnexpectedError() {
        return kind.isUnexpectedError();
    }

    private enum Kind {
        SUCCESS, FAILURE, EMAIL_TAKEN, SPAM, DENIED, EMAIL_INVALID, FLAKY_SIGNUP_ERROR, DEVICE_CONFLICT, UNAUTHORIZED, NETWORK_ERROR, SERVER_ERROR, VALIDATION_ERROR;

        private static EnumSet<Kind> UNEXPECTED_ERRORS = EnumSet.of(FAILURE, FLAKY_SIGNUP_ERROR, SERVER_ERROR, VALIDATION_ERROR);

        public boolean isUnexpectedError() {
            return UNEXPECTED_ERRORS.contains(this);
        }
    }

    private AuthTaskResult(PublicApiUser user, SignupVia signupVia, boolean showFacebookSuggestions) {
        this(Kind.SUCCESS, user, signupVia, null, showFacebookSuggestions, null, null);
    }

    private AuthTaskResult(Exception exception) {
        this(Kind.FAILURE, null, null, exception, false, null, null);
    }

    private AuthTaskResult(Kind kind, String serverErrorMessage, ApiRequestException exception) {
        this(kind, null, null, exception, false, null, serverErrorMessage);
    }

    private AuthTaskResult(Kind kind, Exception exception) {
        this(kind, null, null, exception, false, null, null);
    }

    private AuthTaskResult(@NotNull Kind kind, PublicApiUser user, SignupVia signupVia,
                           Exception exception, boolean showFacebookSuggestions, Bundle loginBundle,
                           String serverErrorMessage) {
        this.kind = kind;
        this.user = user;
        this.signupVia = signupVia;
        this.exception = exception;
        this.showFacebookSuggestions = showFacebookSuggestions;
        this.loginBundle = loginBundle;
        this.serverErrorMessage = serverErrorMessage;
    }

    public boolean wasSuccess() {
        return kind == Kind.SUCCESS;
    }

    public boolean wasFailure() {
        return kind == Kind.FAILURE;
    }

    public boolean wasEmailTaken() {
        return kind == Kind.EMAIL_TAKEN;
    }

    public boolean wasSpam() {
        return kind == Kind.SPAM;
    }

    public boolean wasDenied() {
        return kind == Kind.DENIED;
    }

    public boolean wasUnauthorized() {
        return kind == Kind.UNAUTHORIZED;
    }

    public boolean wasServerError() {
        return kind == Kind.SERVER_ERROR;
    }

    public boolean wasNetworkError() {
        return kind == Kind.NETWORK_ERROR;
    }

    public boolean wasEmailInvalid() {
        return kind == Kind.EMAIL_INVALID;
    }

    public boolean wasSignUpFailedToLogin() {
        return kind == Kind.FLAKY_SIGNUP_ERROR;
    }

    public boolean wasDeviceConflict() {
        return kind == Kind.DEVICE_CONFLICT;
    }

    public boolean wasValidationError() {
        return kind == Kind.VALIDATION_ERROR;
    }

    public PublicApiUser getUser() {
        return user;
    }

    public SignupVia getSignupVia() {
        return signupVia;
    }

    public Exception getException() {
        return exception;
    }

    public boolean getShowFacebookSuggestions() {
        return showFacebookSuggestions;
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
