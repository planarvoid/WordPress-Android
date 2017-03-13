package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.storage.Tables.Charts;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.ChartTracks;
import com.soundcloud.android.tracks.TrackArtwork;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.ListMultiMap;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;
import rx.functions.Func1;

import android.support.annotation.NonNull;
import android.util.Pair;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ChartsStorage {
    private final PropellerRx propellerRx;

    private final Func1<List<Pair<Chart, TrackArtwork>>, List<Chart>> TO_CHARTS_WITH_TRACKS = chartsWithTracks -> {
        final MultiMap<Chart, TrackArtwork> chartToArtworkMap = new ListMultiMap<>();
        for (final Pair<Chart, TrackArtwork> chartWithTrack : chartsWithTracks) {
            chartToArtworkMap.put(chartWithTrack.first, chartWithTrack.second);
        }
        final List<Chart> result = new ArrayList<>(chartToArtworkMap.keySet().size());
        for (final Chart chart : chartToArtworkMap.keySet()) {
            final List<TrackArtwork> trackArtworks = Lists.newArrayList(chartToArtworkMap.get(chart));
            result.add(chart.copyWithTrackArtworks(trackArtworks));
        }
        Collections.sort(result, (lhs, rhs) -> lhs.localId().compareTo(rhs.localId()));
        return result;
    };

    @Inject
    ChartsStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    Observable<ChartBucket> featuredCharts() {
        final Where isFeaturedChart = filter().whereIn(Charts.BUCKET_TYPE,
                                                       Charts.BUCKET_TYPE_GLOBAL,
                                                       Charts.BUCKET_TYPE_FEATURED_GENRES);
        final Query query = chartsWithTracksQuery().where(isFeaturedChart);
        return propellerRx.query(query)
                          .map(new ChartWithTrackMapper())
                          .toList()
                          .map(TO_CHARTS_WITH_TRACKS)
                          .map(toChartBucket());
    }

    Observable<List<Chart>> genres(ChartCategory chartCategory) {
        final Query query = chartsWithTracksQuery().whereEq(Charts.BUCKET_TYPE, Charts.BUCKET_TYPE_ALL_GENRES)
                                                   .whereEq(Charts.CATEGORY, chartCategory.value());
        return propellerRx.query(query).map(new ChartWithTrackMapper()).toList().map(TO_CHARTS_WITH_TRACKS);
    }

    @NonNull
    private Query chartsWithTracksQuery() {
        return Query.from(Charts.TABLE)
                    .select(Charts._ID,
                            Charts.TYPE,
                            Charts.DISPLAY_NAME,
                            Charts.CATEGORY,
                            Charts.GENRE,
                            Charts.BUCKET_TYPE,
                            ChartTracks.TRACK_ID,
                            ChartTracks.TRACK_ARTWORK)
                    .innerJoin(ChartTracks.TABLE, Charts._ID, ChartTracks.CHART_ID);
    }

    private Func1<List<Chart>, ChartBucket> toChartBucket() {
        return charts -> ChartBucket.create(
                newArrayList(filterCharts(charts, ChartBucketType.GLOBAL)),
                newArrayList(filterCharts(charts, ChartBucketType.FEATURED_GENRES))
        );
    }

    private Iterable<Chart> filterCharts(List<Chart> charts, final ChartBucketType type) {
        return Iterables.filter(charts, input -> input.bucketType() == type);
    }

    private static final class ChartWithTrackMapper extends RxResultMapper<Pair<Chart, TrackArtwork>> {
        @Override
        public Pair<Chart, TrackArtwork> map(CursorReader cursorReader) {
            final String genre = cursorReader.getString(Charts.GENRE);
            final Chart chart = Chart.create(cursorReader.getLong(Charts._ID),
                                             ChartType.from(cursorReader.getString(Charts.TYPE)),
                                             ChartCategory.from(cursorReader.getString(Charts.CATEGORY)),
                                             cursorReader.getString(Charts.DISPLAY_NAME),
                                             new Urn(genre),
                                             toChartBucketType(cursorReader.getInt(Charts.BUCKET_TYPE)));
            final TrackArtwork trackArtwork = TrackArtwork.create(
                    Urn.forTrack(cursorReader.getLong(ChartTracks.TRACK_ID)),
                    Optional.fromNullable(cursorReader.getString(ChartTracks.TRACK_ARTWORK)));
            return new Pair<>(chart, trackArtwork);
        }

        private ChartBucketType toChartBucketType(int bucketType) {
            switch (bucketType) {
                case Charts.BUCKET_TYPE_GLOBAL:
                    return ChartBucketType.GLOBAL;
                case Charts.BUCKET_TYPE_FEATURED_GENRES:
                    return ChartBucketType.FEATURED_GENRES;
                case Charts.BUCKET_TYPE_ALL_GENRES:
                    return ChartBucketType.ALL_GENRES;
                default:
                    throw new IllegalArgumentException("Unknown type:" + bucketType);
            }
        }
    }
}
