package com.soundcloud.android.discovery;

import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.Tables.RecommendationSeeds;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.Recommendations;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

class RecommendationsStorage {

    private static final String RECOMMENDATIONS_SOUND_VIEW = "RecommendationsSoundView";

    private final PropellerRx propellerRx;

    @Inject
    RecommendationsStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }



    Observable<Optional<PropertySet>> firstSeed() {
        final Where soundsViewJoin = filter()
                .whereEq(RecommendationSeeds.SEED_SOUND_TYPE, SoundView.field(TableColumns.SoundView._TYPE))
                .whereEq(RecommendationSeeds.SEED_SOUND_ID, SoundView.field(TableColumns.SoundView._ID));

        Query query = Query.from(RecommendationSeeds.TABLE)
                .select(RecommendationSeeds._ID,
                        RecommendationSeeds.SEED_SOUND_ID,
                        RecommendationSeeds.RECOMMENDATION_REASON,
                        field(SoundView.field(TableColumns.SoundView.TITLE)).as(RecommendationSeedMapper.SEED_TITLE))
                .limit(1)
                .order(RecommendationSeeds._ID, Query.Order.ASC)
                .innerJoin(SoundView.name(), soundsViewJoin);

        return propellerRx.query(query)
                .map(new RecommendationSeedMapper())
                .map(RxUtils.<PropertySet>TO_OPTIONAL())
                .switchIfEmpty(Observable.just(Optional.<PropertySet>absent()));
    }

    // TODO in the next PR this needs to return a more constrained list of PropertySet to line up with RecommendationSeedMapper
    Observable<List<PropertySet>> seedTracks() {
        final Where soundsViewJoin = filter()
                .whereEq(RecommendationSeeds.SEED_SOUND_TYPE, SoundView.field(TableColumns.SoundView._TYPE))
                .whereEq(RecommendationSeeds.SEED_SOUND_ID, SoundView.field(TableColumns.SoundView._ID));

        final Where recommendationsJoin = filter()
                .whereEq(RecommendationSeeds._ID, Recommendations.SEED_ID);

        final Where recommendationsViewJoin = filter()
                .whereEq(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView._TYPE, Recommendations.RECOMMENDED_SOUND_TYPE)
                .whereEq(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView._ID, Recommendations.RECOMMENDED_SOUND_ID);

        //TODO: Add Join aliasing to propeller
        Query query = Query.from(RecommendationSeeds.TABLE)
                .select(RecommendationSeeds._ID.as(RecommendationMapper.SEED_LOCAL_ID),
                        RecommendationSeeds.SEED_SOUND_ID,
                        RecommendationSeeds.RECOMMENDATION_REASON,
                        field(SoundView.field(TableColumns.SoundView.TITLE)).as(RecommendationMapper.SEED_TITLE),
                        //TODO: wait for Fernandos branch to land which adds min/max column functions (PR: https://github.com/soundcloud/propeller/pull/58)
                        "MIN(" + Recommendations._ID + ")",
                        field(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView._ID).as(RecommendationMapper.RECOMMENDATION_ID),
                        field(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView.ARTWORK_URL).as(RecommendationMapper.RECOMMENDATION_ARTWORK_URL),
                        field(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView.TITLE).as(RecommendationMapper.RECOMMENDATION_TITLE),
                        field(RECOMMENDATIONS_SOUND_VIEW + "." + TableColumns.SoundView.USERNAME).as(RecommendationMapper.RECOMMENDATION_USERNAME),
                        count(RecommendationSeeds.SEED_SOUND_ID).as(RecommendationMapper.RECOMMENDATION_COUNT))

                .innerJoin(SoundView.name(), soundsViewJoin)
                .innerJoin(Recommendations.TABLE, recommendationsJoin)
                .innerJoin(SoundView.name() + " AS " + RECOMMENDATIONS_SOUND_VIEW, recommendationsViewJoin)
                .groupBy(RecommendationSeeds.SEED_SOUND_ID)
                .order(Recommendations._ID, Query.Order.ASC);

        return propellerRx.query(query).map(new RecommendationMapper()).toList();
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
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.SNIPPET_DURATION,
                        TableColumns.SoundView.FULL_DURATION,
                        TableColumns.SoundView.PLAYBACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.CREATED_AT)

                .innerJoin(RecommendationSeeds.TABLE, recommendationsJoin)
                .innerJoin(SoundView.name(), soundsViewJoin)
                .whereEq(Recommendations.SEED_ID, localSeedId);

        return propellerRx.query(query).map(new RecommendedTrackMapper()).toList();
    }

    Observable<List<Urn>> recommendedTracks() {
        final Query query = allRecommendedTracks();

        return propellerRx.query(query).map(new TrackUrnMapper()).toList();
    }

    Observable<List<Urn>> recommendedTracksBeforeSeed(long localSeedId) {
        final Query query = allRecommendedTracks().whereLt(Recommendations.SEED_ID, localSeedId);

        return propellerRx.query(query).map(new TrackUrnMapper()).toList();
    }

    Observable<List<Urn>> recommendedTracksAfterSeed(long localSeedId) {
        final Query query = allRecommendedTracks().whereGe(Recommendations.SEED_ID, localSeedId);

        return propellerRx.query(query).map(new TrackUrnMapper()).toList();
    }

    private Query allRecommendedTracks() {
        return Query.from(Recommendations.TABLE)
                .select(Recommendations.RECOMMENDED_SOUND_ID)
                .whereEq(Recommendations.RECOMMENDED_SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
    }

    private static final class TrackUrnMapper extends RxResultMapper<Urn> {
        @Override
        public Urn map(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(Recommendations.RECOMMENDED_SOUND_ID));
        }
    }

    private static final class RecommendationSeedMapper extends RxResultMapper<PropertySet> {
        static final String SEED_TITLE = "seed_title";

        @Override
        public PropertySet map(CursorReader cursorReader) {
            PropertySet propertySet = PropertySet.create(cursorReader.getColumnCount());
            propertySet.put(RecommendationProperty.SEED_TRACK_LOCAL_ID, cursorReader.getLong(RecommendationSeeds._ID));
            propertySet.put(RecommendationProperty.SEED_TRACK_URN, Urn.forTrack(cursorReader.getLong(RecommendationSeeds.SEED_SOUND_ID)));
            propertySet.put(RecommendationProperty.SEED_TRACK_TITLE, cursorReader.getString(SEED_TITLE));
            propertySet.put(RecommendationProperty.REASON, getReason(cursorReader.getInt(RecommendationSeeds.RECOMMENDATION_REASON)));

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
}
