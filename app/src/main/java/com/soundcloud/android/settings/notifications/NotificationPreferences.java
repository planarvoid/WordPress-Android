package com.soundcloud.android.settings.notifications;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class NotificationPreferences {

    private final HashMap<String, NotificationPreference> properties;

    public NotificationPreferences() {
        properties = new HashMap<>(12);
    }

    @JsonAnySetter
    public void add(String key, NotificationPreference preference) {
        properties.put(key, preference);
    }

    @JsonAnyGetter
    public Map<String, NotificationPreference> getProperties() {
        return properties;
    }

}

