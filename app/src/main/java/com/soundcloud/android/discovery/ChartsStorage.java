package com.soundcloud.android.discovery;

import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.SoundView.ARTWORK_URL;
import static com.soundcloud.android.storage.TableColumns.SoundView._ID;
import static com.soundcloud.android.storage.Tables.Charts;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.ChartTracks;
import com.soundcloud.android.tracks.TrackArtwork;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class ChartsStorage {
    private final PropellerRx propellerRx;
    private final Scheduler scheduler;

    private final Func1<Chart, Observable<Chart>> addTrackArtworksToChart = new Func1<Chart, Observable<Chart>>() {
        @Override
        public Observable<Chart> call(final Chart chart) {
            return trackArtworks(chart.localId())
                    .map(new Func1<List<TrackArtwork>, Chart>() {
                        @Override
                        public Chart call(List<TrackArtwork> trackArtworks) {
                            return chart.copyWithTrackArtworks(trackArtworks);
                        }
                    });
        }
    };

    @Inject
    ChartsStorage(PropellerRx propellerRx, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.propellerRx = propellerRx;
        this.scheduler = scheduler;
    }

    Observable<ChartBucket> charts() {
        final Query query = Query.from(Charts.TABLE)
                                 .select(Charts._ID,
                                         Charts.TYPE,
                                         Charts.DISPLAY_NAME,
                                         Charts.CATEGORY,
                                         Charts.GENRE,
                                         Charts.BUCKET_TYPE);

        return propellerRx.query(query)
                          .map(new ChartMapper())
                          .flatMap(addTrackArtworksToChart)
                          .toList()
                          .map(toChartBucket())
                          .subscribeOn(scheduler);
    }

    private Func1<List<Chart>, ChartBucket> toChartBucket() {
        return new Func1<List<Chart>, ChartBucket>() {
            @Override
            public ChartBucket call(List<Chart> charts) {
                return ChartBucket.create(
                        newArrayList(filterCharts(charts, ChartBucketType.GLOBAL)),
                        newArrayList(filterCharts(charts, ChartBucketType.FEATURED_GENRES))
                );
            }
        };
    }

    private Iterable<Chart> filterCharts(List<Chart> charts, final ChartBucketType type) {
        return Iterables.filter(charts, new Predicate<Chart>() {
            @Override
            public boolean apply(Chart input) {
                return input.bucketType() == type;
            }
        });
    }

    private Observable<List<TrackArtwork>> trackArtworks(final Long chartId) {

        final Where soundsViewJoin = filter().whereEq(ChartTracks.SOUND_ID, SoundView.field(_ID));
        final Query query = Query.from(ChartTracks.TABLE)
                                 .select(ChartTracks.SOUND_ID, field(SoundView.field(ARTWORK_URL)).as(ARTWORK_URL))
                                 .innerJoin(SoundView.name(), soundsViewJoin)
                                 .whereEq(ChartTracks.CHART_ID, chartId);

        return propellerRx
                .query(query)
                .map(new TrackArtworkMapper())
                .toList();
    }

    private static final class TrackArtworkMapper extends RxResultMapper<TrackArtwork> {
        @Override
        public TrackArtwork map(CursorReader cursorReader) {
            return TrackArtwork.create(Urn.forTrack(cursorReader.getLong(ChartTracks.SOUND_ID)),
                                       Optional.fromNullable(cursorReader.getString(ARTWORK_URL)));
        }
    }

    private static final class ChartMapper extends RxResultMapper<Chart> {
        @Override
        public Chart map(CursorReader cursorReader) {
            final String genre = cursorReader.getString(Charts.GENRE);
            return Chart.create(cursorReader.getLong(Charts._ID),
                                ChartType.from(cursorReader.getString(Charts.TYPE)),
                                ChartCategory.from(cursorReader.getString(Charts.CATEGORY)),
                                cursorReader.getString(Charts.DISPLAY_NAME),
                                new Urn(genre),
                                toChartBucketType(cursorReader.getInt(Charts.BUCKET_TYPE)));
        }

        private ChartBucketType toChartBucketType(int bucketType) {
            switch (bucketType) {
                case Charts.BUCKET_TYPE_GLOBAL:
                    return ChartBucketType.GLOBAL;
                case Charts.BUCKET_TYPE_FEATURED_GENRE:
                    return ChartBucketType.FEATURED_GENRES;
                default:
                    throw new IllegalArgumentException("Unknown type:" + bucketType);
            }
        }
    }
}
