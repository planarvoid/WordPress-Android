package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.RecommendationSeeds;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

class RecommendationMapper extends RxResultMapper<PropertySet> {

    static final String SEED_LOCAL_ID = "seed_local_id";
    static final String SEED_TITLE = "seed_title";

    static final String RECOMMENDATION_TITLE = "recommendation_title";
    static final String RECOMMENDATION_USERNAME = "recommendation_username";
    static final String RECOMMENDATION_ID = "recommendation_id";
    static final String RECOMMENDATION_ARTWORK_URL = "recommendation_ARTWORK_URL";
    static final String RECOMMENDATION_COUNT = "recommendations_count";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(RecommendationProperty.SEED_TRACK_LOCAL_ID, cursorReader.getLong(SEED_LOCAL_ID));
        propertySet.put(RecommendationProperty.SEED_TRACK_URN, Urn.forTrack(cursorReader.getLong(RecommendationSeeds.SEED_SOUND_ID)));
        propertySet.put(RecommendationProperty.SEED_TRACK_TITLE, cursorReader.getString(SEED_TITLE));
        propertySet.put(RecommendationProperty.RECOMMENDED_TRACKS_COUNT, cursorReader.getInt(RECOMMENDATION_COUNT));
        propertySet.put(RecommendationProperty.REASON, getReason(cursorReader.getInt(RecommendationSeeds.RECOMMENDATION_REASON)));
        propertySet.put(RecommendedTrackProperty.URN, Urn.forTrack(cursorReader.getLong(RECOMMENDATION_ID)));
        propertySet.put(RecommendedTrackProperty.IMAGE_URL_TEMPLATE, Optional.fromNullable(cursorReader.getString(RECOMMENDATION_ARTWORK_URL)));
        propertySet.put(RecommendedTrackProperty.TITLE, cursorReader.getString(RECOMMENDATION_TITLE));
        propertySet.put(RecommendedTrackProperty.USERNAME, cursorReader.getString(RECOMMENDATION_USERNAME));
        return propertySet;
    }

    private RecommendationReason getReason(int dbReason) {
        switch (dbReason) {
            case RecommendationSeeds.REASON_LIKED:
                return RecommendationReason.LIKED;
            case RecommendationSeeds.REASON_LISTENED_TO:
                return RecommendationReason.LISTENED_TO;
            default:
                throw new IllegalStateException("Could not find reason for database value " + dbReason);
        }
    }
}
