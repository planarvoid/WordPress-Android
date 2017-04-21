package com.soundcloud.android.olddiscovery.recommendations;


import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class RecommendationBucketRendererTest extends AndroidUnitTest {
    private static final int BUCKET_POSITION = 0;
    private static final long SEED_ID = 1;
    private static final int QUERY_POSITION = 2;
    private static final RecommendationReason REASON = RecommendationReason.LIKED;
    private static final ApiTrack SEED_TRACK = ModelFixtures.create(ApiTrack.class);
    private static final TrackItem RECOMMENDED_TRACK = ModelFixtures.trackItem();
    private static final Recommendation RECOMMENDATION = Recommendation.create(RECOMMENDED_TRACK,
                                                                               SEED_TRACK.getUrn(),
                                                                               false,
                                                                               QUERY_POSITION,
                                                                               Urn.NOT_SET);

    @Mock private Navigator navigator;
    @Mock private RecommendationRendererFactory recommendationRendererFactory;
    @Mock private TrackRecommendationListener listener;
    @Mock private RecommendationRenderer recommendationRenderer;
    @Mock private PerformanceMetricsEngine performanceMetricsEngine;

    @Before
    public void setUp() {
        when(recommendationRendererFactory.create(listener)).thenReturn(recommendationRenderer);
    }

    @Test
    public void shouldBindHeadingToViewIfViewAllBucket() {
        final List<RecommendedTracksBucketItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createViewAllRenderer();
        final View itemView = createItemView(renderer);
        renderer.bindItemView(BUCKET_POSITION, itemView, recommendedTracksItems);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendations_header))
                .containsText(itemView.getResources().getString(R.string.recommendation_seeds_header))
                .isVisible();
    }

    @Test
    public void shouldHideHeadingIfNonViewAllBucket() {
        final List<RecommendedTracksBucketItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createDefaultRenderer();
        final View itemView = createItemView(renderer);

        renderer.bindItemView(BUCKET_POSITION, itemView, recommendedTracksItems);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendations_header)).isGone();
    }

    @Test
    public void shouldBindReasonToView() {
        final List<RecommendedTracksBucketItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createViewAllRenderer();
        final View itemView = createItemView(renderer);

        renderer.bindItemView(BUCKET_POSITION, itemView, recommendedTracksItems);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.reason))
                .containsText("Because you liked " + SEED_TRACK.getTitle())
                .isVisible();
    }

    @Test
    public void tappingOnSeedTrackShouldPropagateEventToListener() {
        final List<RecommendedTracksBucketItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createViewAllRenderer();
        final View itemView = createItemView(renderer);

        renderer.bindItemView(BUCKET_POSITION, itemView, recommendedTracksItems);

        ButterKnife.<TextView>findById(itemView, R.id.reason).performClick();

        verify(listener).onReasonClicked(SEED_TRACK.getUrn());
    }

    @Test
    public void shouldBindViewAllToViewWhenViewAllBucket() {
        final List<RecommendedTracksBucketItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createViewAllRenderer();
        final View itemView = createItemView(renderer);

        renderer.bindItemView(BUCKET_POSITION, itemView, recommendedTracksItems);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendations_view_all_text)).containsText(
                itemView.getResources().getString(R.string.recommendation_view_all));
    }

    @Test
    public void shouldHideViewAllWhenNonViewAllBucket() {
        final List<RecommendedTracksBucketItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createDefaultRenderer();
        final View itemView = createItemView(renderer);

        renderer.bindItemView(BUCKET_POSITION, itemView, recommendedTracksItems);

        assertThat(ButterKnife.<View>findById(itemView, R.id.recommendations_view_all)).isGone();
    }

    @Test
    public void onSeeAllButtonClickShouldStartLoadingMeasurement() {
        final RecommendationBucketRenderer renderer = createDefaultRenderer();
        renderer.onViewAllButtonClick(mock(View.class));

        verify(performanceMetricsEngine).startMeasuring(MetricType.SUGGESTED_TRACKS_LOAD);
    }

    private RecommendationBucketRenderer createViewAllRenderer() {
        return new RecommendationBucketRenderer(true,
                                                listener,
                                                recommendationRendererFactory,
                                                navigator,
                                                performanceMetricsEngine);
    }

    private RecommendationBucketRenderer createDefaultRenderer() {
        return new RecommendationBucketRenderer(false,
                                                listener,
                                                recommendationRendererFactory,
                                                navigator,
                                                performanceMetricsEngine);
    }

    private View createItemView(RecommendationBucketRenderer renderer) {
        return renderer.createItemView(new FrameLayout(context()));
    }

    private RecommendationSeed createSeed() {
        return RecommendationSeed.create(
                SEED_ID,
                SEED_TRACK.getUrn(),
                SEED_TRACK.getTitle(),
                REASON,
                QUERY_POSITION,
                Urn.NOT_SET
        );
    }

    private List<RecommendedTracksBucketItem> createRecommendationsBucket() {
        final RecommendedTracksBucketItem recommendedTracksItem = RecommendedTracksBucketItem.create(
                createSeed(),
                Collections.singletonList(RECOMMENDATION));
        return Collections.singletonList(recommendedTracksItem);
    }

}
