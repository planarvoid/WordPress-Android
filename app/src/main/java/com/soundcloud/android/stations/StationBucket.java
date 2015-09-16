package com.soundcloud.android.stations;

import rx.functions.Func1;

import java.util.Collections;
import java.util.List;

class StationBucket {

    private final String title;
    private final int collectionType;
    private final int bucketSize;
    private final List<StationViewModel> stationViewModels;

    public StationBucket(String title, final int collectionType, int bucketSize, List<StationViewModel> stationViewModels) {
        this.title = title;
        this.collectionType = collectionType;
        this.bucketSize = bucketSize;
        this.stationViewModels = Collections.unmodifiableList(stationViewModels);
    }

    public List<StationViewModel> getStationViewModels() {
        return stationViewModels;
    }

    public String getTitle() {
        return title;
    }

    public int getCollectionType() {
        return collectionType;
    }

    public int getBucketSize() {
        return bucketSize;
    }

    public static Func1<List<StationViewModel>, StationBucket> fromStationViewModels(final String name, final int collectionType, final int bucketSize) {
        return new Func1<List<StationViewModel>, StationBucket>() {
            @Override
            public StationBucket call(List<StationViewModel> stationViewModels) {
                return new StationBucket(name, collectionType, bucketSize, stationViewModels);
            }
        };
    }
}
