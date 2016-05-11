package com.soundcloud.android.discovery;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSubscribers;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscoveryPresenterTest extends AndroidUnitTest {

    private static final Urn SEED_TRACK_URN = Urn.forTrack(123L);
    private static final TrackItem RECOMMENDED_TRACK = TrackItem.from(ModelFixtures.create(ApiTrack.class));
    private static final List<Urn> TRACKLIST = Arrays.asList(SEED_TRACK_URN, RECOMMENDED_TRACK.getUrn());

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private DiscoveryAdapter adapter;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;
    @Mock private RecommendationBucket recommendationBucketOne;
    @Mock private FeatureFlags featureFlags;
    @Mock private ChartsPresenter chartsPresenter;

    private DiscoveryPresenter presenter;
    private Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider = TestSubscribers.expandPlayerSubscriber();

    @Before
    public void setUp() {
        this.presenter = new DiscoveryPresenter(
                swipeRefreshAttacher,
                discoveryOperations,
                adapter,
                imagePauseOnScrollListener,
                expandPlayerSubscriberProvider,
                playbackInitiator,
                navigator,
                featureFlags, chartsPresenter);

        when(recommendationBucketOne.getSeedTrackUrn()).thenReturn(SEED_TRACK_URN);
        when(recommendationBucketOne.getRecommendations()).thenReturn(Collections.singletonList(RECOMMENDED_TRACK));
        when(featureFlags.isEnabled(Flag.DISCOVERY_RECOMMENDATIONS)).thenReturn(true);
        when(playbackInitiator.playTracks(anyListOf(Urn.class), any(Integer.class), any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult.success()));
    }

    @Test
    public void clickOnRecommendationReasonPlaysSeedAndRecommendedTracksStartingFromBeginning() {
        presenter.onReasonClicked(recommendationBucketOne);

        verify(playbackInitiator).playTracks(TRACKLIST, 0, new PlaySessionSource(Screen.RECOMMENDATIONS_MAIN));
    }

    @Test
    public void clickOnRecommendationArtworkPlaysRecommendedTracksStartingFromSelectedTrack() {
        presenter.onRecommendationClicked(recommendationBucketOne, RECOMMENDED_TRACK);

        verify(playbackInitiator).playTracks(TRACKLIST, 1, new PlaySessionSource(Screen.RECOMMENDATIONS_MAIN));
    }

    @Test
    public void tagSelectedOpensPlaylistDiscoveryActivity() {
        final String playListTag = "playListTag";
        final Context context = context();

        presenter.onTagSelected(context, playListTag);

        verify(navigator).openPlaylistDiscoveryTag(context, playListTag);
    }
}
