package com.soundcloud.android.discovery.recommendations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.rx.RxResultMapper;

final class RecommendationSeedMapper extends RxResultMapper<RecommendationSeed> {
    static final String SEED_TITLE = "seed_title";

    @Override
    public RecommendationSeed map(CursorReader cursorReader) {
        final long seedTrackLocalId = cursorReader.getLong(Tables.RecommendationSeeds._ID);
        final Urn seedTrackUrn = Urn.forTrack(cursorReader.getLong(Tables.RecommendationSeeds.SEED_SOUND_ID));
        final String seedTrackTitle = cursorReader.getString(SEED_TITLE);
        final RecommendationReason reason = getReason(cursorReader.getInt(Tables.RecommendationSeeds.RECOMMENDATION_REASON));
        final int queryPosition = cursorReader.getInt(Tables.RecommendationSeeds.QUERY_POSITION);
        final Urn queryUrn = new Urn(cursorReader.getString(Tables.RecommendationSeeds.QUERY_URN));

        return RecommendationSeed.create(seedTrackLocalId, seedTrackUrn, seedTrackTitle, reason, queryPosition, queryUrn);
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
