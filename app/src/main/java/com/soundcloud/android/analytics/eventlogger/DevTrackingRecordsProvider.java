package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.analytics.TrackingRecord;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import android.support.v4.util.CircularArray;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class DevTrackingRecordsProvider {

    private static final int MAX_SIZE = 50;
    private static final int NUM_OF_ELEMENTS = 1;

    private final CircularArray<TrackingRecord> trackingRecords = new CircularArray<>();
    private final BehaviorSubject<Action> actionSubject = BehaviorSubject.createDefault(Action.DEFAULT);

    enum Action {
        DEFAULT,
        ADD,
        DELETE_ALL
    }

    @Inject
    DevTrackingRecordsProvider() {}

    void add(TrackingRecord trackingRecord) {
        if (trackingRecords.size() == MAX_SIZE) {
            trackingRecords.removeFromStart(NUM_OF_ELEMENTS);
        }
        trackingRecords.addLast(trackingRecord);
        actionSubject.onNext(Action.ADD);
    }

    void deleteAll() {
        trackingRecords.clear();
        actionSubject.onNext(Action.DELETE_ALL);
    }

    CircularArray<TrackingRecord> latest() {
        return trackingRecords;
    }

    Observable<Action> action() {
        return actionSubject;
    }
}
