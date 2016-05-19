package com.soundcloud.android.stations;

import com.soundcloud.android.R;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

final class StationTypes {
    private static final String TRACK = "track";
    private static final String CURATOR = "curator";
    private static final String GENRE = "genre";
    private static final String ARTIST = "artist";

    static String getHumanReadableType(Resources resources, String type) {
        switch (type) {
            case StationTypes.TRACK:
                return resources.getString(R.string.station_type_track);
            case StationTypes.GENRE:
                return resources.getString(R.string.station_type_genre);
            case StationTypes.CURATOR:
                return resources.getString(R.string.station_type_curator);
            case StationTypes.ARTIST:
                return resources.getString(R.string.station_type_artist);
            default:
                return Strings.EMPTY;
        }
    }
}
