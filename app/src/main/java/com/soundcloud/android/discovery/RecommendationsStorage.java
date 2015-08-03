package com.soundcloud.android.discovery;

import static com.soundcloud.android.storage.Tables.RecommendationSeeds;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.Recommendations;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class RecommendationsStorage {

    private static final String RECOMMENDATIONS_SOUND_VIEW = "RecommendationsSoundView";
    private final PropellerRx propellerRx;

    @Inject
    public RecommendationsStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    Observable<List<PropertySet>> seedTracks() {

        final Where soundViewJoin = filter()
                .whereEq(RecommendationSeeds.SEED_SOUND_TYPE, Table.SoundView.field(TableColumns.SoundView._TYPE))
                .whereEq(RecommendationSeeds.SEED_SOUND_ID, Table.SoundView.field(TableColumns.SoundView._ID));

        final Where recommendationsJoin = filter().whereEq(RecommendationSeeds._ID, Recommendations.SEED_ID);

        final Where recommendationsViewJoin = filter()
                .whereEq(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView._TYPE, Recommendations.RECOMMENDED_SOUND_TYPE)
                .whereEq(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView._ID, Recommendations.RECOMMENDED_SOUND_ID);

        //TODO: Add Min functionality to propeller: PR: https://github.com/soundcloud/propeller/pull/58
        //TODO: Add Join aliasing to propeller
        Query query = Query.from(RecommendationSeeds.TABLE)
                .select(RecommendationSeeds._ID.as(SeedSoundMapper.SEED_LOCAL_ID),
                        RecommendationSeeds.SEED_SOUND_ID,
                        RecommendationSeeds.RECOMMENDATION_REASON,
                        field(Table.SoundView.field(TableColumns.SoundView.TITLE)).as(SeedSoundMapper.SEED_TITLE),
                        // TODO: wait for Fernandos branch to land which adds min/max column functions
                        "MIN(" + Recommendations._ID + ")",
                        field(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView._ID).as(SeedSoundMapper.RECOMMENDATION_ID),
                        field(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView.TITLE).as(SeedSoundMapper.RECOMMENDATION_TITLE),
                        field(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView.USERNAME).as(SeedSoundMapper.RECOMMENDATION_USERNAME),
                        count(RecommendationSeeds.SEED_SOUND_ID).as(SeedSoundMapper.RECOMMENDATION_COUNT))

                .innerJoin(Table.SoundView.name(), soundViewJoin)
                .innerJoin(Recommendations.TABLE, recommendationsJoin)
                .innerJoin(Table.SoundView.name() + " AS " + RECOMMENDATIONS_SOUND_VIEW, recommendationsViewJoin)
                .groupBy(RecommendationSeeds.SEED_SOUND_ID)
                .order(Recommendations._ID, Query.Order.ASC);

        return propellerRx.query(query).map(new SeedSoundMapper()).toList();
    }

    Observable<List<Urn>> recommendations(long seedId) {

        Query query = Query.from(Recommendations.TABLE)
                .select(Recommendations.RECOMMENDED_SOUND_ID)
                .whereEq(Recommendations.SEED_ID, seedId);

        return propellerRx.query(query).map(new RecommendationTrackUrnMapper()).toList();
    }

    public final class RecommendationTrackUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(Recommendations.RECOMMENDED_SOUND_ID));
        }
    }
}
