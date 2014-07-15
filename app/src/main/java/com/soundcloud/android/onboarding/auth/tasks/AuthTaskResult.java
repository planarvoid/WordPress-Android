package com.soundcloud.android.onboarding.auth.tasks;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.onboarding.auth.SignupVia;

public class AuthTaskResult {
    private boolean     success;
    private PublicApiUser user;
    private SignupVia   signupVia;
    private Exception   exception;

    public static AuthTaskResult success(PublicApiUser user, SignupVia signupVia) {
        return new AuthTaskResult(user,signupVia);
    }

    public static AuthTaskResult failure(Exception e) {
        return new AuthTaskResult(e);
    }

    public static AuthTaskResult failure(String errorMessage) {
        return failure(new AuthTaskException(errorMessage));
    }

    private AuthTaskResult(PublicApiUser user, SignupVia signupVia) {
        success = true;
        this.user = user;
        this.signupVia = signupVia;
    }

    private AuthTaskResult(Exception e){
        exception = e;
        success = false;
    }

    public boolean wasSuccess() {
        return success;
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
