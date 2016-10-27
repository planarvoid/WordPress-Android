package com.soundcloud.android.discovery.recommendations;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.TableColumns.SoundView;
import com.soundcloud.android.storage.Tables.Recommendations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

class RecommendedTrackMapper extends RxResultMapper<PropertySet> {

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(RecommendedTrackProperty.SEED_SOUND_URN,
                        Urn.forTrack(cursorReader.getLong(Recommendations.SEED_ID)));
        propertySet.put(PlayableProperty.URN, Urn.forTrack(cursorReader.getLong(Recommendations.RECOMMENDED_SOUND_ID)));
        propertySet.put(PlayableProperty.TITLE, cursorReader.getString(SoundView.TITLE));
        propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(SoundView.USERNAME));
        propertySet.put(PlayableProperty.CREATOR_URN,
                        Urn.forUser(cursorReader.getLong(TableColumns.SoundView.USER_ID)));
        propertySet.put(TrackProperty.SNIPPET_DURATION, cursorReader.getLong(SoundView.SNIPPET_DURATION));
        propertySet.put(TrackProperty.FULL_DURATION, cursorReader.getLong(SoundView.FULL_DURATION));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(SoundView.PLAYBACK_COUNT));
        propertySet.put(PlayableProperty.LIKES_COUNT, cursorReader.getInt(SoundView.LIKES_COUNT));
        propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(SoundView.CREATED_AT));
        propertySet.put(TrackProperty.SUB_HIGH_TIER, cursorReader.getBoolean(SoundView.POLICIES_SUB_HIGH_TIER));
        propertySet.put(TrackProperty.SNIPPED, cursorReader.getBoolean(SoundView.POLICIES_SNIPPED));
        return propertySet;
    }
}
