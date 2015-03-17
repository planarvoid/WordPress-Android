package com.soundcloud.android.analytics;

import java.util.List;

interface TrackingApi {
    List<TrackingRecord> pushToRemote(List<TrackingRecord> events);
}
