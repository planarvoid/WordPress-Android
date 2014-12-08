package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;

public class AuthTaskResult {
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

    private final Kind kind;
    private final PublicApiUser user;
    private final SignupVia signupVia;
    private final Exception exception;
    private final boolean showFacebookSuggestions;

    private enum Kind {
        SUCCESS, FAILURE, EMAIL_TAKEN, SPAM, DENIED, EMAIL_INVALID
    }

    private AuthTaskResult(PublicApiUser user, SignupVia signupVia, boolean showFacebookSuggestions) {
        this(Kind.SUCCESS, user, signupVia, null, showFacebookSuggestions);
    }

    private AuthTaskResult(Exception exception) {
        this(Kind.FAILURE, null, null, exception, false);

    }

    private AuthTaskResult(Kind kind) {
        this(kind, null, null, null, false);
    }

    private AuthTaskResult(Kind kind, PublicApiUser user, SignupVia signupVia, Exception exception, boolean showFacebookSuggestions) {
        this.kind = kind;
        this.user = user;
        this.signupVia = signupVia;
        this.exception = exception;
        this.showFacebookSuggestions = showFacebookSuggestions;
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

    public String[] getErrors() {
        return exception instanceof AuthTaskException ? ((AuthTaskException) exception).getErrors() : null;
    }
}
