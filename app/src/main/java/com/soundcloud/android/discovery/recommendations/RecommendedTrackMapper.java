package com.soundcloud.android.discovery.recommendations;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.Recommendations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

class RecommendedTrackMapper extends RxResultMapper<PropertySet> {

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(RecommendedTrackProperty.SEED_SOUND_URN, Urn.forTrack(cursorReader.getLong(Recommendations.SEED_ID)));
        propertySet.put(PlayableProperty.URN, Urn.forTrack(cursorReader.getLong(Recommendations.RECOMMENDED_SOUND_ID)));
        propertySet.put(PlayableProperty.TITLE, cursorReader.getString(Tables.TrackView.TITLE));
        propertySet.put(PlayableProperty.CREATOR_URN, Urn.forUser(cursorReader.getLong(Tables.TrackView.CREATOR_ID)));
        propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(Tables.TrackView.CREATOR_NAME));
        propertySet.put(TrackProperty.SNIPPET_DURATION, cursorReader.getLong(Tables.TrackView.SNIPPET_DURATION));
        propertySet.put(TrackProperty.FULL_DURATION, cursorReader.getLong(Tables.TrackView.FULL_DURATION));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(Tables.TrackView.PLAY_COUNT));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(Tables.TrackView.LIKES_COUNT));
        propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(Tables.TrackView.CREATED_AT));
        propertySet.put(TrackProperty.SUB_HIGH_TIER, cursorReader.getBoolean(Tables.TrackView.SUB_HIGH_TIER));
        propertySet.put(TrackProperty.SNIPPED, cursorReader.getBoolean(Tables.TrackView.SNIPPED));
        propertySet.put(TrackProperty.IS_USER_LIKE, cursorReader.getBoolean(Tables.TrackView.IS_USER_LIKE));
        propertySet.put(TrackProperty.IS_USER_REPOST, cursorReader.getBoolean(Tables.TrackView.IS_USER_REPOST));
        propertySet.put(TrackProperty.PERMALINK_URL, cursorReader.getString(Tables.TrackView.PERMALINK_URL));
        return propertySet;
    }
}
