package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;

import android.os.Bundle;

public final class AuthTaskResult {
    public static AuthTaskResult success(PublicApiUser user, SignupVia signupVia, boolean showFacebookSuggestions) {
        return new AuthTaskResult(user, signupVia, showFacebookSuggestions);
    }

    public static AuthTaskResult failure(Exception exception) {
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
        return new AuthTaskResult(Kind.DEVICE_CONFLICT, null, null, null, false, loginBundle);
    }

    private final Kind kind;
    private final PublicApiUser user;
    private final SignupVia signupVia;
    private final Exception exception;

    private final boolean showFacebookSuggestions;
    private final Bundle loginBundle;

    private enum Kind {
        SUCCESS, FAILURE, EMAIL_TAKEN, SPAM, DENIED, EMAIL_INVALID, FLAKY_SIGNUP_ERROR, DEVICE_CONFLICT
    }

    private AuthTaskResult(PublicApiUser user, SignupVia signupVia, boolean showFacebookSuggestions) {
        this(Kind.SUCCESS, user, signupVia, null, showFacebookSuggestions, null);
    }

    private AuthTaskResult(Exception exception) {
        this(Kind.FAILURE, null, null, exception, false, null);
    }

    private AuthTaskResult(Kind kind) {
        this(kind, null, null, null, false, null);
    }

    private AuthTaskResult(Kind kind, PublicApiUser user, SignupVia signupVia,
                           Exception exception, boolean showFacebookSuggestions, Bundle loginBundle) {
        this.kind = kind;
        this.user = user;
        this.signupVia = signupVia;
        this.exception = exception;
        this.showFacebookSuggestions = showFacebookSuggestions;
        this.loginBundle = loginBundle;
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

    public String[] getErrors() {
        return exception instanceof AuthTaskException ? ((AuthTaskException) exception).getErrors() : null;
    }
}
