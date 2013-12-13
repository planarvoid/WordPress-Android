package com.soundcloud.android.analytics;

import java.util.List;
import java.util.Map;

public interface LocalyticsEvent {
    public String getTag();
    public Map<String, String> getAttributes();
    public List<String> getCustomDimensions();
}
