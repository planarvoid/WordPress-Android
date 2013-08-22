package com.soundcloud.android.task.auth;

import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.model.User;

public class AuthTaskResult {
    private boolean     success;
    private User        user;
    private SignupVia   signupVia;
    private Exception   exception;

    public static AuthTaskResult success(User user, SignupVia signupVia) {
        return new AuthTaskResult(user,signupVia);
    }

    public static AuthTaskResult failure(Exception e) {
        return new AuthTaskResult(e);
    }

    public static AuthTaskResult failure(String errorMessage) {
        return failure(new AuthTaskException(errorMessage));
    }

    private AuthTaskResult(User user, SignupVia signupVia) {
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

    public User getUser() {
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
