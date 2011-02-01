
package com.soundcloud.utils;

import org.urbanstew.soundcloudapi.AuthorizationURLOpener;

public interface SoundCloudAuthorizationClient extends AuthorizationURLOpener {
    public enum AuthorizationStatus {
        SUCCESSFUL, CANCELED, FAILED
    }

    String getVerificationCode();

    void authorizationCompleted(AuthorizationStatus status);

    void exceptionOccurred(Exception e);
}
