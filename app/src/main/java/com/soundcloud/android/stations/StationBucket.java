package com.soundcloud.android.stations;

import rx.functions.Func1;

import java.util.Collections;
import java.util.List;

class StationBucket {

    private final List<Station> stations;
    private final String title;

    public StationBucket(String title, List<Station> stations) {
        this.title = title;
        this.stations = Collections.unmodifiableList(stations);
    }

    public List<Station> getStations() {
        return stations;
    }

    public String getTitle() {
        return title;
    }

    public static Func1<List<Station>, StationBucket> fromStations(final String name) {
        return new Func1<List<Station>, StationBucket>() {
            @Override
            public StationBucket call(List<Station> stations) {
                return new StationBucket(name, stations);
            }
        };
    }
}
