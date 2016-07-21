package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.sync.charts.ApiImageResource;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.assertj.core.util.Lists;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ChartsFixtures {

    static ApiChart<ApiImageResource> createChartWithImageResources(ChartType type, ChartCategory chartCategory) {
        final List<ApiImageResource> apiImageResources = Lists.newArrayList();
        apiImageResources.add(ApiImageResource.create(Urn.forTrack(1), null));
        final ModelCollection<ApiImageResource> chartTracks = new ModelCollection<>(apiImageResources);
        return new ApiChart<>("title", new Urn("soundcloud:genre:all"), type, chartCategory, new Date(12345L), chartTracks);
    }

    static ApiChart<ApiTrack> createApiChart(String genre, ChartType type) {
        final ApiTrack chartTrack = ModelFixtures.create(ApiTrack.class);
        return new ApiChart<>("title",
                              new Urn("soundcloud:chart:" + genre),
                              type,
                              ChartCategory.MUSIC,
                              new Date(12345L),
                              new ModelCollection<>(Collections.singletonList(chartTrack)));
    }
}
