package com.soundcloud.android.discovery.recommendations;

import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.Tables.RecommendationSeeds;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.Recommendations;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

class RecommendationsStorage {

    private final PropellerRx propellerRx;

    @Inject
    RecommendationsStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    Observable<PropertySet> firstSeed() {
        return loadSeeds(seedsQuery().limit(1));
    }

    Observable<PropertySet> allSeeds() {
        return loadSeeds(seedsQuery());
    }

    private Observable<PropertySet> loadSeeds(Query query) {
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

    Observable<List<PropertySet>> recommendedTracksForSeed(long localSeedId) {
        final Where recommendationsJoin = filter()
                .whereEq(RecommendationSeeds._ID, Recommendations.SEED_ID);

        final Where soundsViewJoin = filter()
                .whereEq(Recommendations.RECOMMENDED_SOUND_TYPE, SoundView.field(TableColumns.SoundView._TYPE))
                .whereEq(Recommendations.RECOMMENDED_SOUND_ID, SoundView.field(TableColumns.SoundView._ID));

        final Query query = Query.from(Recommendations.TABLE)
                                 .select(Recommendations.SEED_ID,
                                         Recommendations.RECOMMENDED_SOUND_ID,
                                         TableColumns.SoundView.TITLE,
                                         TableColumns.SoundView.USER_ID,
                                         TableColumns.SoundView.USERNAME,
                                         TableColumns.SoundView.SNIPPET_DURATION,
                                         TableColumns.SoundView.FULL_DURATION,
                                         TableColumns.SoundView.PLAYBACK_COUNT,
                                         TableColumns.SoundView.LIKES_COUNT,
                                         TableColumns.SoundView.CREATED_AT,
                                         TableColumns.SoundView.POLICIES_SUB_HIGH_TIER,
                                         TableColumns.SoundView.POLICIES_SNIPPED)

                                 .innerJoin(RecommendationSeeds.TABLE, recommendationsJoin)
                                 .innerJoin(SoundView.name(), soundsViewJoin)
                                 .whereEq(Recommendations.SEED_ID, localSeedId);

        return propellerRx.query(query).map(new RecommendedTrackMapper()).toList();
    }
}
