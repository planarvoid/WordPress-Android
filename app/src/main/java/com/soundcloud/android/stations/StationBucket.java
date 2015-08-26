package com.soundcloud.android.stations;

import rx.functions.Func1;

import java.util.Collections;
import java.util.List;

class StationBucket {

    private final String title;
    private final int bucketSize;
    private final List<Station> stations;

    public StationBucket(String title, int bucketSize, List<Station> stations) {
        this.title = title;
        this.bucketSize = bucketSize;
        this.stations = Collections.unmodifiableList(stations);
    }

    public List<Station> getStations() {
        return stations;
    }

    public String getTitle() {
        return title;
    }

    public int getBucketSize() {
        return bucketSize;
    }

    public static Func1<List<Station>, StationBucket> fromStations(final String name, final int bucketSize) {
        return new Func1<List<Station>, StationBucket>() {
            @Override
            public StationBucket call(List<Station> stations) {
                return new StationBucket(name, bucketSize, stations);
            }
        };
    }
}
