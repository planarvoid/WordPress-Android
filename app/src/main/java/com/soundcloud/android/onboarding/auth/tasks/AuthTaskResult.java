package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;

public class AuthTaskResult {

    private final Kind  kind;
    private PublicApiUser        user;
    private SignupVia   signupVia;
    private Exception   exception;

    public enum Kind {
        SUCCESS, FAILURE, EMAIL_TAKEN, SPAM, DENIED, EMAIL_INVALID
    }

    public static AuthTaskResult success(PublicApiUser user, SignupVia signupVia) {
        return new AuthTaskResult(user,signupVia);
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

    private AuthTaskResult(PublicApiUser user, SignupVia signupVia) {
        this.kind = Kind.SUCCESS;
        this.user = user;
        this.signupVia = signupVia;
    }

    private AuthTaskResult(Exception exception){
        this.kind = Kind.FAILURE;
        this.exception = exception;
    }

    public AuthTaskResult(Kind kind) {
        this.kind = kind;
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

    public String[] getErrors(){
        return exception instanceof AuthTaskException ? ((AuthTaskException) exception).getErrors() : null;
    }
}
