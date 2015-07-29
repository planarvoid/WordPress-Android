package com.soundcloud.android.recommendations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

class SeedSoundMapper extends RxResultMapper<PropertySet> {

    public static final String SEED_TITLE = "seed_title";

    public static final String RECOMMENDATION_TITLE = "recommendation_title";
    public static final String RECOMMENDATION_USERNAME = "recommendation_username";
    public static final String RECOMMENDATION_ID = "recommendation_id";
    public static final String RECOMMENDATION_COUNT = "recommendations_count";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        final PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(SeedSoundProperty.URN, Urn.forTrack(cursorReader.getLong(TableColumns.RecommendationSeeds.SEED_SOUND_ID)));
        propertySet.put(SeedSoundProperty.TITLE, cursorReader.getString(SEED_TITLE));
        propertySet.put(SeedSoundProperty.RECOMMENDATION_COUNT, cursorReader.getInt(RECOMMENDATION_COUNT));
        propertySet.put(SeedSoundProperty.REASON, getReason(cursorReader.getInt(TableColumns.RecommendationSeeds.RECOMMENDATION_REASON)));
        propertySet.put(RecommendationProperty.URN, Urn.forTrack(cursorReader.getLong(RECOMMENDATION_ID)));
        propertySet.put(RecommendationProperty.TITLE, cursorReader.getString(RECOMMENDATION_TITLE));
        propertySet.put(RecommendationProperty.USERNAME, cursorReader.getString(RECOMMENDATION_USERNAME));
        return propertySet;
    }

    private RecommendationReason getReason(int dbReason) {
        switch (dbReason) {
            case TableColumns.RecommendationSeeds.REASON_LIKED:
                return RecommendationReason.LIKED;
            case TableColumns.RecommendationSeeds.REASON_LISTENED_TO:
                return RecommendationReason.LISTENED_TO;
            default:
                throw new IllegalStateException("Could not find reason for database value " + dbReason);
        }
    }
}
