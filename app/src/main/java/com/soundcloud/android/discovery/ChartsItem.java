package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
abstract class ChartsItem extends DiscoveryItem {
    protected ChartsItem() {
        super(Kind.ChartItem);
    }

    static ChartsItem from(ChartBucket chartBucket) {
        final Optional<Chart> newAndHotChart = Iterables.tryFind(chartBucket.getGlobal(), isType(ChartType.TRENDING));
        final Optional<Chart> topFiftyChart = Iterables.tryFind(chartBucket.getGlobal(), isType(ChartType.TOP));

        final Optional<Chart> firstGenreChart = getGenreAtIfPresent(chartBucket, 0);
        final Optional<Chart> secondGenreChart = getGenreAtIfPresent(chartBucket, 1);
        final Optional<Chart> thirdGenreChart = getGenreAtIfPresent(chartBucket, 2);

        return new AutoValue_ChartsItem(
                newAndHotChart,
                topFiftyChart,
                firstGenreChart,
                secondGenreChart,
                thirdGenreChart);
    }

    private static Optional<Chart> getGenreAtIfPresent(ChartBucket chartBucket, int index) {
        final List<Chart> featuredGenres = chartBucket.getFeaturedGenres();
        return featuredGenres.size() > index
               ? Optional.of(featuredGenres.get(index))
               : Optional.<Chart>absent();
    }

    private static Predicate<Chart> isType(final ChartType chartType) {
        return new Predicate<Chart>() {
            @Override
            public boolean apply(Chart input) {
                return input.type() == chartType;
            }
        };
    }

    abstract Optional<Chart> newAndHotChart();

    abstract Optional<Chart> topFiftyChart();

    abstract Optional<Chart> firstGenreChart();

    abstract Optional<Chart> secondGenreChart();

    abstract Optional<Chart> thirdGenreChart();
}
