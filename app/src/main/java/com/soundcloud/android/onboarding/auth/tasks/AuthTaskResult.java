package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;

import android.os.Bundle;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import static com.soundcloud.android.utils.Log.ONBOARDING_TAG;

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
        if (exception.reason() == ApiRequestException.Reason.VALIDATION_ERROR) {
            // TODO: going forward, the key should be mapped to individual Reasons
            return new AuthTaskResult(Kind.VALIDATION_ERROR, exception.errorKey());
        }
        return new AuthTaskResult(exception);
    }

    public static AuthTaskResult failure(String errorMessage) {
        return failure(new AuthTaskException(errorMessage));
    }

    public static AuthTaskResult emailTaken() {
        return new AuthTaskResult(Kind.EMAIL_TAKEN);
    }

    public static AuthTaskResult spam() {
        return new AuthTaskResult(Kind.SPAM);
    }

    public static AuthTaskResult denied() {
        return new AuthTaskResult(Kind.DENIED);
    }

    public static AuthTaskResult emailInvalid() {
        return new AuthTaskResult(Kind.EMAIL_INVALID);
    }

    public static AuthTaskResult signUpFailedToLogin() {
        return new AuthTaskResult(Kind.FLAKY_SIGNUP_ERROR);
    }

    public static AuthTaskResult deviceConflict(Bundle loginBundle) {
        return new AuthTaskResult(Kind.DEVICE_CONFLICT, null, null, null, false, loginBundle, null);
    }

    private enum Kind {
        SUCCESS, FAILURE, EMAIL_TAKEN, SPAM, DENIED, EMAIL_INVALID, FLAKY_SIGNUP_ERROR, DEVICE_CONFLICT, VALIDATION_ERROR
    }

    private AuthTaskResult(PublicApiUser user, SignupVia signupVia, boolean showFacebookSuggestions) {
        this(Kind.SUCCESS, user, signupVia, null, showFacebookSuggestions, null, null);
    }

    private AuthTaskResult(Exception exception) {
        this(Kind.FAILURE, null, null, exception, false, null, null);
    }

    private AuthTaskResult(Kind kind) {
        this(kind, null, null, null, false, null, null);
    }

    private AuthTaskResult(Kind kind, String serverErrorMessage) {
        this(kind, null, null, null, false, null, serverErrorMessage);
    }

    private AuthTaskResult(@NotNull Kind kind, PublicApiUser user, SignupVia signupVia,
                           Exception exception, boolean showFacebookSuggestions, Bundle loginBundle,
                           String serverErrorMessage) {
        String message = String.format(
                "Creating auth task result with\n\tkind: %s\n\tuser present: %b\n\tvia: %s\n\texception: %s\n\tbundle present: %b\n\tserver error: %s",
                kind,
                user != null,
                signupVia,
                exception,
                loginBundle != null,
                serverErrorMessage
        );
        Log.w(ONBOARDING_TAG, message);

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

    public String getKindString() {
        return kind.toString();
    }
}
