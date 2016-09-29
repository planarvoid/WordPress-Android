package com.soundcloud.android.sync.timeline;

import rx.Observable;

import java.util.List;

public interface TimelineStorage<T> {

    Observable<T> timelineItems(int limit);

    Observable<T> timelineItemsBefore(long timestamp, int limit);

    List<T> timelineItemsSince(long timestamp, int limit);

    Observable<Integer> timelineItemCountSince(long timestamp);
}
