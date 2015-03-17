package com.soundcloud.android.analytics;

import java.util.List;

public interface TrackingApi {
    List<TrackingRecord> pushToRemote(List<TrackingRecord> events);
}
