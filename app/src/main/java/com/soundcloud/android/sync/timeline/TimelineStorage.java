package com.soundcloud.android.sync.timeline;

import com.soundcloud.java.collections.PropertySet;
import rx.Observable;

import java.util.List;

public interface TimelineStorage {

    Observable<PropertySet> timelineItems(int limit);

    Observable<PropertySet> timelineItemsBefore(long timestamp, int limit);

    List<PropertySet> timelineItemsSince(long timestamp, int limit);
}
