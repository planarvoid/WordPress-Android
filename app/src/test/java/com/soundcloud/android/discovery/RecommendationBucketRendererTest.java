package com.soundcloud.android.discovery;


import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import butterknife.ButterKnife;
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

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecommendationBucketRendererTest extends AndroidUnitTest {
    private static final long SEED_ID = 1;
    private static final RecommendationReason REASON = RecommendationReason.LIKED;
    private final static ApiTrack SEED_TRACK = ModelFixtures.create(ApiTrack.class);
    private final static TrackItem RECOMMENDED_TRACK = TrackItem.from(ModelFixtures.create(ApiTrack.class));
    private final static Recommendation RECOMMENDATION_VIEW_MODEL = new Recommendation(RECOMMENDED_TRACK, SEED_TRACK.getUrn(), false);
    private final static List<Urn> TRACKS_LIST = Arrays.asList(SEED_TRACK.getUrn(), RECOMMENDED_TRACK.getUrn());

    @Mock private ImageOperations imageOperations;
    @Mock private TrackItemMenuPresenter trackItemMenuPresenter;
    @Mock private RecommendationsAdapterFactory adapterFactory;
    @Mock private RecommendationsAdapter adapter;
    @Mock private PlaybackInitiator playbackInitiator;
    private Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider = TestSubscribers.expandPlayerSubscriber();

    private View itemView;
    private RecommendationBucketRenderer viewAllRenderer;
    private RecommendationBucketRenderer defaultRenderer;

    @Before
    public void setUp() {
        when(playbackInitiator.playTracks(anyListOf(Urn.class), any(Integer.class), any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult.success()));
        when(adapterFactory.create(any(Screen.class))).thenReturn(adapter);

        itemView = LayoutInflater.from(fragmentActivity()).inflate(R.layout.recommendation_bucket, new FrameLayout(context()), false);
        viewAllRenderer = new RecommendationBucketRenderer(Screen.RECOMMENDATIONS_MAIN, true, adapterFactory, playbackInitiator, expandPlayerSubscriberProvider);
        defaultRenderer = new RecommendationBucketRenderer(Screen.RECOMMENDATIONS_MAIN, false, adapterFactory, playbackInitiator, expandPlayerSubscriberProvider);
    }

    @Test
    public void shouldBindHeadingToViewIfViewAllBucket() {
        final List<RecommendationBucket> recommendationBuckets = createRecommendationsBucket();
        viewAllRenderer.bindItemView(0, itemView, recommendationBuckets);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendations_header))
                .containsText(itemView.getResources().getString(R.string.recommendation_seeds_header))
                .isVisible();
    }

    @Test
    public void shouldHideHeadingIfNonViewAllBucket() {
        final List<RecommendationBucket> recommendationBuckets = createRecommendationsBucket();
        defaultRenderer.bindItemView(0, itemView, recommendationBuckets);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendations_header)).isGone();
    }

    @Test
    public void shouldBindReasonToView() {
        final List<RecommendationBucket> recommendationBuckets = createRecommendationsBucket();
        viewAllRenderer.bindItemView(0, itemView, recommendationBuckets);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.reason))
                .containsText("Because you liked " + SEED_TRACK.getTitle())
                .isVisible();
    }

    @Test
    public void tappingOnSeedTrackShouldTriggerRecommendationsBucketListenerIfAvailable() {
        final List<RecommendationBucket> recommendationBuckets = createRecommendationsBucket();
        viewAllRenderer.bindItemView(0, itemView, recommendationBuckets);
        final TextView reasonView = ButterKnife.findById(itemView, R.id.reason);

        reasonView.performClick();
        verify(playbackInitiator).playTracks(TRACKS_LIST, 0, new PlaySessionSource(Screen.RECOMMENDATIONS_MAIN));
    }

    @Test
    public void shouldBindViewAllToViewWhenViewAllBucket() {
        final List<RecommendationBucket> recommendationBuckets = createRecommendationsBucket();
        viewAllRenderer.bindItemView(0, itemView, recommendationBuckets);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendations_view_all_text)).containsText(itemView.getResources().getString(R.string.recommendation_view_all));
    }

    @Test
    public void shouldHideViewAllWhenNonViewAllBucket() {
        final List<RecommendationBucket> recommendationBuckets = createRecommendationsBucket();
        defaultRenderer.bindItemView(0, itemView, recommendationBuckets);

        assertThat(ButterKnife.findById(itemView, R.id.recommendations_view_all)).isGone();
    }

    private PropertySet createSeed() {
        return PropertySet.from(
                RecommendationProperty.SEED_TRACK_LOCAL_ID.bind(SEED_ID),
                RecommendationProperty.SEED_TRACK_URN.bind(SEED_TRACK.getUrn()),
                RecommendationProperty.SEED_TRACK_TITLE.bind(SEED_TRACK.getTitle()),
                RecommendationProperty.REASON.bind(REASON)
        );
    }

    private List<RecommendationBucket> createRecommendationsBucket() {
        final RecommendationBucket recommendationBucket = new RecommendationBucket(createSeed(), Collections.singletonList(RECOMMENDATION_VIEW_MODEL));
        return Collections.singletonList(recommendationBucket);
    }
}
