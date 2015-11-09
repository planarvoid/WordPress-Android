package com.soundcloud.android.sync.timeline;

import com.soundcloud.java.collections.PropertySet;
import rx.Observable;

public interface TimelineStorage {

    Observable<PropertySet> timelineItems(int limit);

    Observable<PropertySet> timelineItemsBefore(long timestamp, int limit);

}
