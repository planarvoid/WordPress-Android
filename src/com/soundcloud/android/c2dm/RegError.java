package com.soundcloud.android.c2dm;


/**
 * see <a href="http://code.google.com/android/c2dm/">Android Cloud to Device Messaging Framework</a>
 */
enum RegError {
    /**
     * The device can't read the response, or there was a 500/503 from the server that
     * can be retried later. The application should use exponential back off and retry.
     */
    SERVICE_NOT_AVAILABLE,


    /**
     * There is no Google account on the phone. The application should ask the user to open
     * the account manager and add a Google account. Fix on the device side.
     */
    ACCOUNT_MISSING,

    /**
     * Bad password. The application should ask the user to enter his/her password, and let user
     * retry manually later. Fix on the device side.
     */
    AUTHENTICATION_FAILED,

    /**
     * The user has too many applications registered. The application should tell the user
     * to uninstall some other applications, let user retry manually. Fix on the device side.
     */
    TOO_MANY_REGISTRATIONS,

    /**
     * The sender account is not recognized.
     */
    INVALID_SENDER,

    /**
     * Incorrect phone registration with Google. This phone doesn't currently support C2DM
     */
    PHONE_REGISTRATION_ERROR,


    /**
     * Error not listed in official documentation
     */
    UNKNOWN_ERROR;

    static RegError fromString(String s) {
        try {
            return RegError.valueOf(s);
        } catch (IllegalArgumentException e) {
            return UNKNOWN_ERROR;
        }
    }
}