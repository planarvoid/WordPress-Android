package com.soundcloud.android.discovery.recommendations;

import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.Tables.RecommendationSeeds;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
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

    Observable<List<PropertySet>> recommendedTracksForSeed(long localSeedId) {
        final Where recommendationsJoin = filter()
                .whereEq(RecommendationSeeds._ID, Recommendations.SEED_ID);

        final Where soundsViewJoin = filter()
                .whereEq(Recommendations.RECOMMENDED_SOUND_ID, Tables.TrackView.ID.qualifiedName());

        final Query query = Query.from(Recommendations.TABLE)
                                 .select(Recommendations.SEED_ID,
                                         Recommendations.RECOMMENDED_SOUND_ID,
                                         Tables.TrackView.TITLE,
                                         Tables.TrackView.CREATOR_ID,
                                         Tables.TrackView.CREATOR_NAME,
                                         Tables.TrackView.SNIPPET_DURATION,
                                         Tables.TrackView.FULL_DURATION,
                                         Tables.TrackView.PLAY_COUNT,
                                         Tables.TrackView.LIKES_COUNT,
                                         Tables.TrackView.CREATED_AT,
                                         Tables.TrackView.SUB_HIGH_TIER,
                                         Tables.TrackView.SNIPPED,
                                         Tables.TrackView.IS_USER_LIKE,
                                         Tables.TrackView.IS_USER_REPOST,
                                         Tables.TrackView.PERMALINK_URL)

                                 .innerJoin(RecommendationSeeds.TABLE, recommendationsJoin)
                                 .innerJoin(Tables.TrackView.TABLE, soundsViewJoin)
                                 .whereEq(Recommendations.SEED_ID, localSeedId);

        return propellerRx.query(query).map(new RecommendedTrackMapper()).toList();
    }
}
