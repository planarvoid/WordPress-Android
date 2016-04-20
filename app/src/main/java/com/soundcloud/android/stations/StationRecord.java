package com.soundcloud.android.stations;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;

import java.util.List;

public interface StationRecord extends ImageResource {

    Function<StationRecord, Urn> TO_URN = new Function<StationRecord, Urn>() {
        @Override
        public Urn apply(StationRecord station) {
            return station.getUrn();
        }
    };

    List<StationTrack> getTracks();

    String getType();

    String getTitle();

    String getPermalink();

    int getPreviousPosition();

}
