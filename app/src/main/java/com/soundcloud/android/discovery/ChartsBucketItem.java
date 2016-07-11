package com.soundcloud.android.discovery;

import static com.soundcloud.android.api.model.ChartType.TOP;
import static com.soundcloud.android.api.model.ChartType.TRENDING;
import static com.soundcloud.java.collections.Iterables.tryFind;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
abstract class ChartsBucketItem extends DiscoveryItem {
    private static final Function<Chart, ChartListItem> TO_PRESENTATION_MODEL = new Function<Chart, ChartListItem>() {
        public ChartListItem apply(Chart chart) {
            return new ChartListItem(chart.trackArtworks(), chart.genre(), chart.displayName(), chart.bucketType(),
                                     chart.type());
        }
    };

    protected ChartsBucketItem() {
        super(Kind.ChartItem);
    }

    static ChartsBucketItem from(ChartBucket chartBucket) {
        final Optional<ChartListItem> newAndHotChart = tryFind(chartBucket.getGlobal(), isType(TRENDING)).transform(
                TO_PRESENTATION_MODEL);
        final Optional<ChartListItem> topFiftyChart = tryFind(chartBucket.getGlobal(), isType(TOP)).transform(
                TO_PRESENTATION_MODEL);

        final Optional<ChartListItem> firstGenreChart = getGenreAtIfPresent(chartBucket, 0);
        final Optional<ChartListItem> secondGenreChart = getGenreAtIfPresent(chartBucket, 1);
        final Optional<ChartListItem> thirdGenreChart = getGenreAtIfPresent(chartBucket, 2);

        return new AutoValue_ChartsBucketItem(
                newAndHotChart,
                topFiftyChart,
                firstGenreChart,
                secondGenreChart,
                thirdGenreChart);
    }

    private static Optional<ChartListItem> getGenreAtIfPresent(ChartBucket chartBucket, int index) {
        final List<Chart> featuredGenres = chartBucket.getFeaturedGenres();
        return featuredGenres.size() > index
               ? Optional.of(featuredGenres.get(index)).transform(TO_PRESENTATION_MODEL)
               : Optional.<ChartListItem>absent();
    }

    private static Predicate<Chart> isType(final ChartType chartType) {
        return new Predicate<Chart>() {
            @Override
            public boolean apply(Chart input) {
                return input.type() == chartType;
            }
        };
    }

    abstract Optional<ChartListItem> newAndHotChart();

    abstract Optional<ChartListItem> topFiftyChart();

    abstract Optional<ChartListItem> firstGenreChart();

    abstract Optional<ChartListItem> secondGenreChart();

    abstract Optional<ChartListItem> thirdGenreChart();
}
