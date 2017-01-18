package com.soundcloud.android.cast;

import org.json.JSONObject;

public class LoadMessageParameters {
    public boolean autoplay;
    public long playPosition;
    public JSONObject jsonData;

    public LoadMessageParameters(boolean autoplay, long playPosition, JSONObject jsonData) {
        this.autoplay = autoplay;
        this.playPosition = playPosition;
        this.jsonData = jsonData;
    }
}
