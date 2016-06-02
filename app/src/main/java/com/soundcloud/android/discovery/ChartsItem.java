package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class ChartsItem extends DiscoveryItem {
    protected ChartsItem() {
        super(Kind.ChartItem);
    }

    static ChartsItem create(Chart newAndHotChart,
                             Chart topFiftyChart,
                             Chart firstGenreChart,
                             Chart secondGenreChart,
                             Chart thirdGenreChart) {
        return new AutoValue_ChartsItem(newAndHotChart,
                                        topFiftyChart,
                                        firstGenreChart,
                                        secondGenreChart,
                                        thirdGenreChart);
    }

    abstract Chart newAndHotChart();

    abstract Chart topFiftyChart();

    abstract Chart firstGenreChart();

    abstract Chart secondGenreChart();

    abstract Chart thirdGenreChart();
}
