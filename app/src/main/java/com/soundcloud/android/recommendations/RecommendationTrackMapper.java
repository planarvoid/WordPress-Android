package com.soundcloud.android.recommendations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

public class RecommendationTrackMapper extends RxResultMapper<PropertySet> {

    @Override
    public PropertySet map(CursorReader cursorReader) {
        // if we introduce playlists, we will need to change this to account for 2 different types

        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(TrackProperty.URN, Urn.forTrack(cursorReader.getLong(TableColumns.Recommendations.RECOMMENDED_SOUND_ID)));
        propertySet.put(TrackProperty.TITLE, cursorReader.getString(TableColumns.SoundView.TITLE));
        propertySet.put(TrackProperty.CREATOR_NAME, cursorReader.getString(TableColumns.SoundView.USERNAME));
        propertySet.put(RecommendationProperty.SEED_SOUND_URN, Urn.forTrack(cursorReader.getLong(TableColumns.RecommendationSeeds.SEED_SOUND_ID)));
        return propertySet;
    }
}
