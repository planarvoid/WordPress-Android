package com.soundcloud.android.discovery;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

public class RecommendedTrackMapper extends RxResultMapper<PropertySet> {

    public static final String RECOMMENDED_TRACK_SEED_ID = "recommended_track_seed_id";
    public static final String RECOMMENDED_TRACK_ID = "recommended_track_id";
    public static final String RECOMMENDED_TRACK_TITLE = "recommended_track_title";
    public static final String RECOMMENDED_TRACK_USERNAME = "recommended_track_username";
    public static final String RECOMMENDED_TRACK_DURATION = "recommended_track_duration";
    public static final String RECOMMENDED_TRACK_PLAYBACK_COUNT = "recommended_track_playback_count";
    public static final String RECOMMENDED_TRACK_LIKES_COUNT = "recommended_track_likes_count";
    public static final String RECOMMENDED_TRACK_CREATED_AT = "recommended_track_created_at";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(RecommendedTrackProperty.SEED_SOUND_URN, Urn.forTrack(cursorReader.getLong(RECOMMENDED_TRACK_SEED_ID)));
        propertySet.put(PlayableProperty.URN, Urn.forTrack(cursorReader.getLong(RECOMMENDED_TRACK_ID)));
        addTitle(cursorReader, propertySet);
        propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(RECOMMENDED_TRACK_USERNAME));
        propertySet.put(PlayableProperty.DURATION, cursorReader.getLong(RECOMMENDED_TRACK_DURATION));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(RECOMMENDED_TRACK_PLAYBACK_COUNT));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(RECOMMENDED_TRACK_LIKES_COUNT));
        propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(RECOMMENDED_TRACK_CREATED_AT));
        return propertySet;
    }

    private void addTitle(CursorReader cursorReader, PropertySet propertySet) {
        final String string = cursorReader.getString(RECOMMENDED_TRACK_TITLE);
        if (string == null) {
            ErrorUtils.handleSilentException("Recommended track", new IllegalStateException("Unexpected null title"));
            propertySet.put(PlayableProperty.TITLE, ScTextUtils.EMPTY_STRING);
        } else {
            propertySet.put(PlayableProperty.TITLE, string);
        }
    }
}
