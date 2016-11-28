package com.soundcloud.android.discovery.charts;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.TrackArtwork;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Collections;

public class ChartListItemRendererTest extends AndroidUnitTest {
    private final String DEFAULT_DISPLAY_NAME = "displayName";
    @Mock private Navigator navigator;
    @Mock private ImageOperations imageOperations;
    private View itemView;
    private ChartListItemRenderer renderer;
    private Activity activity;

    @Before
    public void setUp() throws Exception {
        activity = activity();
        itemView = LayoutInflater.from(activity).inflate(
                R.layout.chart_list_item, new FrameLayout(activity), false);
        renderer = new ChartListItemRenderer(resources(), navigator, imageOperations);
    }

    @Test
    public void shouldBindGenreTitle() {
        final ChartListItem chartListItem = createChartListItem(ChartBucketType.FEATURED_GENRES,
                                                                new Urn("soundcloud:genres:rock"));
        renderer.bindChartListItem(itemView, chartListItem, R.id.chart_list_item);
        assertThat((TextView) itemView.findViewById(R.id.title)).hasText("Rock");
    }

    @Test
    public void shouldBindGlobalTitle() {
        final ChartListItem chartListItem = createChartListItem(ChartBucketType.GLOBAL,
                                                                new Urn("soundcloud:genres:all-music"));
        renderer.bindChartListItem(itemView, chartListItem, R.id.chart_list_item);
        assertThat((TextView) itemView.findViewById(R.id.title)).hasText("Top 50");
    }

    @Test
    public void shouldBindDefaultTitle() {
        final ChartListItem chartListItem = createChartListItem(ChartBucketType.FEATURED_GENRES,
                                                                new Urn("soundcloud:genres:non_existent"));
        renderer.bindChartListItem(itemView, chartListItem, R.id.chart_list_item);
        assertThat((TextView) itemView.findViewById(R.id.title)).hasText(DEFAULT_DISPLAY_NAME);
    }

    @Test
    public void clickingGenreItemShouldNavigateToGenreChart() {
        Urn genre = new Urn("soundcloud:genres:rock");
        final ChartListItem chartListItem = createChartListItem(ChartBucketType.FEATURED_GENRES,
                                                                genre);
        renderer.bindChartListItem(itemView, chartListItem, R.id.chart_list_item);
        itemView.findViewById(R.id.chart_list_item).callOnClick();
        verify(navigator).openChart(activity, genre, ChartType.TOP, ChartCategory.MUSIC, "Rock charts");
    }

    @Test
    public void clickingGlobalItemShouldNavigateToGlobalChart() {
        Urn genre = new Urn("soundcloud:genres:all-music");
        final ChartListItem chartListItem = createChartListItem(ChartBucketType.GLOBAL,
                                                                genre);
        renderer.bindChartListItem(itemView, chartListItem, R.id.chart_list_item);
        itemView.findViewById(R.id.chart_list_item).callOnClick();
        verify(navigator).openChart(activity, genre, ChartType.TOP, ChartCategory.MUSIC, "SoundCloud charts");
    }

    private ChartListItem createChartListItem(ChartBucketType chartBucketType, Urn genre) {
        return new ChartListItem(Collections.<TrackArtwork>emptyList(),
                                 genre,
                                 DEFAULT_DISPLAY_NAME,
                                 chartBucketType,
                                 ChartType.TOP,
                                 ChartCategory.MUSIC);
    }
}
