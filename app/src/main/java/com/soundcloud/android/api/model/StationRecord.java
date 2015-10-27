package com.soundcloud.android.api.model;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;

import java.util.List;

public interface StationRecord {

    Function<StationRecord, Urn> TO_URN = new Function<StationRecord, Urn>() {
        @Override
        public Urn apply(StationRecord station) {
            return station.getUrn();
        }
    };

    List<Urn> getTracks();

    Urn getUrn();

    String getType();

    String getTitle();

    String getPermalink();

    int getPreviousPosition();
}
