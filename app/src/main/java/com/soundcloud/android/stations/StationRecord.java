package com.soundcloud.android.stations;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;

import java.util.List;

public interface StationRecord extends ImageResource {

    List<StationTrack> getTracks();

    String getType();

    String getTitle();

    String getPermalink();

    int getPreviousPosition();

}
