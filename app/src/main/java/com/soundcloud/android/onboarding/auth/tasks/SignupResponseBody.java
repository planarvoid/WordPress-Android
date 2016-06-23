package com.soundcloud.android.onboarding.auth.tasks;

/**
 * Refer to http://gists.int.s-cloud.net/gists/1356 for information about this response body structure
 */
public class SignupResponseBody {

    public static final int ERROR_EMAIL_TAKEN = 101;
    public static final int ERROR_DOMAIN_BLACKLISTED = 102;
    public static final int ERROR_CAPTCHA_REQUIRED = 103;
    public static final int ERROR_EMAIL_INVALID = 104;
    public static final int ERROR_OTHER = 105;

    private int error;

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

}
