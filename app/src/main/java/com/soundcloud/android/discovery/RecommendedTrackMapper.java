package com.soundcloud.android.discovery;

import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.Recommendations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

public class RecommendedTrackMapper extends RxResultMapper<PropertySet> {

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(PlayableProperty.URN, Urn.forTrack(cursorReader.getLong(Recommendations.RECOMMENDED_SOUND_ID)));
        addTitle(cursorReader, propertySet);
        propertySet.put(PlayableProperty.DURATION, cursorReader.getLong(TableColumns.SoundView.DURATION));
        propertySet.put(PlayableProperty.CREATOR_NAME, cursorReader.getString(TableColumns.SoundView.USERNAME));
        propertySet.put(PlayableProperty.CREATED_AT, cursorReader.getDateFromTimestamp(TableColumns.SoundView.CREATED_AT));
        propertySet.put(PlayableProperty.IS_PRIVATE,
                Sharing.PRIVATE.name().equalsIgnoreCase(cursorReader.getString(TableColumns.SoundView.SHARING)));
        propertySet.put(TrackProperty.PLAY_COUNT, cursorReader.getInt(TableColumns.SoundView.PLAYBACK_COUNT));
        if (cursorReader.isNotNull(TableColumns.SoundView.POLICIES_SUB_MID_TIER)) {
            propertySet.put(TrackProperty.SUB_MID_TIER, cursorReader.getBoolean(TableColumns.SoundView.POLICIES_SUB_MID_TIER));
        }

        return propertySet;
    }

    private void addTitle(CursorReader cursorReader, PropertySet propertySet) {
        final String string = cursorReader.getString(TableColumns.SoundView.TITLE);
        if (string == null) {
            ErrorUtils.handleSilentException("Recommended track", new IllegalStateException("Unexpected null title"));
            propertySet.put(PlayableProperty.TITLE, ScTextUtils.EMPTY_STRING);
        } else {
            propertySet.put(PlayableProperty.TITLE, string);
        }
    }
}
