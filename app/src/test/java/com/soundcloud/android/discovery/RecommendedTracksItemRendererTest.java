package com.soundcloud.android.discovery;


import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecommendedTracksItemRendererTest extends AndroidUnitTest {
    private static final long SEED_ID = 1;
    private static final int QUERY_POSITION = 1;
    private static final RecommendationReason REASON = RecommendationReason.LIKED;
    private static final ApiTrack SEED_TRACK = ModelFixtures.create(ApiTrack.class);
    private static final TrackItem RECOMMENDED_TRACK = TrackItem.from(ModelFixtures.create(ApiTrack.class));
    private static final Recommendation RECOMMENDATION = new Recommendation(RECOMMENDED_TRACK, SEED_TRACK.getUrn(), false, QUERY_POSITION, Urn.NOT_SET);
    private static final List<Urn> TRACKS_LIST = Arrays.asList(SEED_TRACK.getUrn(), RECOMMENDED_TRACK.getUrn());

    @Mock private ImageOperations imageOperations;
    @Mock private TrackItemMenuPresenter trackItemMenuPresenter;
    @Mock private RecommendationsAdapter adapter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;
    @Mock private RecommendationRendererFactory recommendationRendererFactory;
    @Mock private RecommendationsTracker tracker;

    private Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider = TestSubscribers.expandPlayerSubscriber();

    @Before
    public void setUp() {
        when(playbackInitiator.playTracks(anyListOf(Urn.class), any(Integer.class), any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult.success()));
    }

    @Test
    public void shouldBindHeadingToViewIfViewAllBucket() {
        final List<RecommendedTracksItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createViewAllRenderer();
        final View itemView = createItemView(renderer);
        renderer.bindItemView(0, itemView, recommendedTracksItems);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendations_header))
                .containsText(itemView.getResources().getString(R.string.recommendation_seeds_header))
                .isVisible();
    }

    @Test
    public void shouldHideHeadingIfNonViewAllBucket() {
        final List<RecommendedTracksItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createDefaultRenderer();
        final View itemView = createItemView(renderer);

        renderer.bindItemView(0, itemView, recommendedTracksItems);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendations_header)).isGone();
    }

    @Test
    public void shouldBindReasonToView() {
        final List<RecommendedTracksItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createViewAllRenderer();
        final View itemView = createItemView(renderer);

        renderer.bindItemView(0, itemView, recommendedTracksItems);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.reason))
                .containsText("Because you liked " + SEED_TRACK.getTitle())
                .isVisible();
    }

    @Test
    public void tappingOnSeedTrackShouldTriggerRecommendationsBucketListenerIfAvailable() {
        final List<RecommendedTracksItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createViewAllRenderer();
        final View itemView = createItemView(renderer);

        renderer.bindItemView(0, itemView, recommendedTracksItems);

        ButterKnife.<TextView>findById(itemView, R.id.reason).performClick();

        verify(playbackInitiator).playTracks(TRACKS_LIST, 0, new PlaySessionSource(Screen.SEARCH_MAIN));
    }

    @Test
    public void shouldBindViewAllToViewWhenViewAllBucket() {
        final List<RecommendedTracksItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createViewAllRenderer();
        final View itemView = createItemView(renderer);

        renderer.bindItemView(0, itemView, recommendedTracksItems);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendations_view_all_text)).containsText(
                itemView.getResources().getString(R.string.recommendation_view_all));
    }

    @Test
    public void shouldHideViewAllWhenNonViewAllBucket() {
        final List<RecommendedTracksItem> recommendedTracksItems = createRecommendationsBucket();
        final RecommendationBucketRenderer renderer = createDefaultRenderer();
        final View itemView = createItemView(renderer);

        renderer.bindItemView(0, itemView, recommendedTracksItems);

        assertThat(ButterKnife.findById(itemView, R.id.recommendations_view_all)).isGone();
    }

    private RecommendationBucketRenderer createViewAllRenderer() {
        return new RecommendationBucketRenderer(true,
                                                playbackInitiator,
                                                expandPlayerSubscriberProvider,
                                                recommendationRendererFactory,
                                                navigator,
                                                tracker);
    }

    private RecommendationBucketRenderer createDefaultRenderer() {
        return new RecommendationBucketRenderer(false,
                                                playbackInitiator,
                                                expandPlayerSubscriberProvider,
                                                recommendationRendererFactory,
                                                navigator,
                                                tracker);
    }

    private View createItemView(RecommendationBucketRenderer renderer) {
        return renderer.createItemView(new FrameLayout(context()));
    }

    private PropertySet createSeed() {
        return PropertySet.from(
                RecommendationProperty.SEED_TRACK_LOCAL_ID.bind(SEED_ID),
                RecommendationProperty.SEED_TRACK_URN.bind(SEED_TRACK.getUrn()),
                RecommendationProperty.SEED_TRACK_TITLE.bind(SEED_TRACK.getTitle()),
                RecommendationProperty.REASON.bind(REASON),
                RecommendationProperty.QUERY_POSITION.bind(QUERY_POSITION),
                RecommendationProperty.QUERY_URN.bind(Urn.NOT_SET)
        );
    }

    private List<RecommendedTracksItem> createRecommendationsBucket() {
        final RecommendedTracksItem recommendedTracksItem = new RecommendedTracksItem(createSeed(), Collections.singletonList(
                RECOMMENDATION));
        return Collections.singletonList(recommendedTracksItem);
    }
}
