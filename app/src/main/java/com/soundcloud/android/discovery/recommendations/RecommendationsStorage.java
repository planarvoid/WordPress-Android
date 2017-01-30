package com.soundcloud.android.discovery.recommendations;

import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.Tables.RecommendationSeeds;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.Recommendations;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

class RecommendationsStorage {

    private final PropellerRx propellerRx;

    @Inject
    RecommendationsStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    Observable<RecommendationSeed> firstSeed() {
        return loadSeeds(seedsQuery().limit(1));
    }

    Observable<RecommendationSeed> allSeeds() {
        return loadSeeds(seedsQuery());
    }

    private Observable<RecommendationSeed> loadSeeds(Query query) {
        return propellerRx.query(query).map(new RecommendationSeedMapper());
    }

    private Query seedsQuery() {
        final Where soundsViewJoin = filter()
                .whereEq(RecommendationSeeds.SEED_SOUND_TYPE, SoundView.field(TableColumns.SoundView._TYPE))
                .whereEq(RecommendationSeeds.SEED_SOUND_ID, SoundView.field(TableColumns.SoundView._ID));

        return Query.from(RecommendationSeeds.TABLE)
                    .select(RecommendationSeeds._ID,
                            RecommendationSeeds.SEED_SOUND_ID,
                            RecommendationSeeds.RECOMMENDATION_REASON,
                            field(SoundView.field(TableColumns.SoundView.TITLE)).as(RecommendationSeedMapper.SEED_TITLE),
                            RecommendationSeeds.QUERY_POSITION,
                            RecommendationSeeds.QUERY_URN)
                    .order(RecommendationSeeds._ID, Query.Order.ASC)
                    .innerJoin(SoundView.name(), soundsViewJoin);
    }

    Observable<List<Urn>> recommendedTracksForSeed(long localSeedId) {
        final Where recommendationsJoin = filter()
                .whereEq(RecommendationSeeds._ID, Recommendations.SEED_ID);

        final Query query = Query.from(Recommendations.TABLE)
                                 .select(Recommendations.RECOMMENDED_SOUND_ID)
                                 .innerJoin(RecommendationSeeds.TABLE, recommendationsJoin)
                                 .whereEq(Recommendations.SEED_ID, localSeedId);

        return propellerRx.query(query).map(new RxResultMapper<Urn>() {
            @Override
            public Urn map(CursorReader reader) {
                return Urn.forTrack(reader.getLong(Recommendations.RECOMMENDED_SOUND_ID));
            }
        }).toList();
    }
}
