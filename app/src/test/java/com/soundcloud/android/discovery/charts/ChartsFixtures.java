package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.sync.charts.ApiImageResource;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.assertj.core.util.Lists;

import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ChartsFixtures {

    public static ApiChart<ApiImageResource> createChartWithImageResources(ChartType type, ChartCategory chartCategory) {
        return createChartWithImageResourcesWithTrackUrn(type, chartCategory, Urn.forTrack(1L));
    }

    static ApiChart<ApiImageResource> createChartWithImageResourcesWithTrackUrn(ChartType type,
                                                                                ChartCategory chartCategory,
                                                                                Urn trackUrn) {
        final List<ApiImageResource> apiImageResources = Lists.newArrayList();
        apiImageResources.add(ApiImageResource.create(trackUrn, "artwork:url"));
        final ModelCollection<ApiImageResource> chartTracks = new ModelCollection<>(apiImageResources);
        return new ApiChart<>("title", new Urn("soundcloud:genre:all"), type, chartCategory, new Date(12345L), chartTracks);
    }

    static ApiChart<ApiTrack> createApiChart(String genre, ChartType type) {
        return createApiChart(genre, type, "soundcloud:charts:1234");
    }

    static ApiChart<ApiTrack> createApiChart(String genre, ChartType type, @Nullable String queryUrn) {
        final ApiTrack chartTrack = ModelFixtures.create(ApiTrack.class);
        final ModelCollection<ApiTrack> apiTracks = new ModelCollection<>(Collections.singletonList(chartTrack),
                                                                          new HashMap<String, Link>(),
                                                                          queryUrn);
        return new ApiChart<>("title",
                              new Urn("soundcloud:chart:" + genre),
                              type,
                              ChartCategory.MUSIC,
                              new Date(12345L),
                              apiTracks);
    }
}
