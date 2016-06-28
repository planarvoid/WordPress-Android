package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.PagedCollection;
import com.soundcloud.android.sync.charts.ApiChart;
import rx.functions.Func1;

class PagedChartTracks extends PagedCollection<ApiTrack> {

    private final boolean firstPage;
    private final ChartType chartType;
    private final Long lastUpdated;

    PagedChartTracks(boolean firstPage, ApiChart apiChart) {
        super(apiChart.tracks());
        this.firstPage = firstPage;
        this.chartType = apiChart.type();
        this.lastUpdated = apiChart.lastUpdated();
    }

    public static Func1<ApiChart, PagedChartTracks> fromApiChart(final boolean firstPage) {
        return new Func1<ApiChart, PagedChartTracks>() {
            @Override
            public PagedChartTracks call(ApiChart apiChart) {
                return new PagedChartTracks(firstPage, apiChart);
            }
        };
    }

    boolean firstPage() {
        return firstPage;
    }

    ChartType chartType() {
        return chartType;
    }

    Long lastUpdated() {
        return lastUpdated;
    }
}
