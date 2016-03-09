package com.soundcloud.android.settings.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationPreference {
    private final boolean mobile;
    private final boolean mail;

    @JsonCreator
    public NotificationPreference(@JsonProperty("mobile") boolean mobile,
                                  @JsonProperty("mail") boolean mail) {
        this.mobile = mobile;
        this.mail = mail;
    }

    public boolean isMobile() {
        return mobile;
    }

    public boolean isMail() {
        return mail;
    }

}
