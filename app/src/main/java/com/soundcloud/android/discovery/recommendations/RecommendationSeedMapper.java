package com.soundcloud.android.discovery.recommendations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

final class RecommendationSeedMapper extends RxResultMapper<PropertySet> {
    static final String SEED_TITLE = "seed_title";

    @Override
    public PropertySet map(CursorReader cursorReader) {
        PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
        propertySet.put(RecommendationProperty.SEED_TRACK_LOCAL_ID,
                        cursorReader.getLong(Tables.RecommendationSeeds._ID));
        propertySet.put(RecommendationProperty.SEED_TRACK_URN,
                        Urn.forTrack(cursorReader.getLong(Tables.RecommendationSeeds.SEED_SOUND_ID)));
        propertySet.put(RecommendationProperty.SEED_TRACK_TITLE, cursorReader.getString(SEED_TITLE));
        propertySet.put(RecommendationProperty.REASON,
                        getReason(cursorReader.getInt(Tables.RecommendationSeeds.RECOMMENDATION_REASON)));
        propertySet.put(RecommendationProperty.QUERY_POSITION,
                        cursorReader.getInt(Tables.RecommendationSeeds.QUERY_POSITION));
        propertySet.put(RecommendationProperty.QUERY_URN,
                        new Urn(cursorReader.getString(Tables.RecommendationSeeds.QUERY_URN)));

        return propertySet;
    }

    private RecommendationReason getReason(int dbReason) {
        switch (dbReason) {
            case Tables.RecommendationSeeds.REASON_LIKED:
                return RecommendationReason.LIKED;
            case Tables.RecommendationSeeds.REASON_PLAYED:
                return RecommendationReason.PLAYED;
            default:
                throw new IllegalStateException("Could not find reason for database value " + dbReason);
        }
    }
}
