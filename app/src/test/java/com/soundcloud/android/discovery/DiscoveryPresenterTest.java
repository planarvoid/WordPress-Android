package com.soundcloud.android.discovery;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ImagePauseOnScrollListener;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiscoveryPresenterTest extends AndroidUnitTest {

    private static final Urn SEED_TRACK_URN = Urn.forTrack(123L);
    private static final TrackItem RECOMMENDED_TRACK = TrackItem.from(ModelFixtures.create(ApiTrack.class));
    private static final Recommendation RECOMMENDATION = new Recommendation(RECOMMENDED_TRACK, SEED_TRACK_URN, false);
    private static final List<Urn> TRACKLIST = Arrays.asList(SEED_TRACK_URN, RECOMMENDED_TRACK.getUrn());

    @Mock private SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock private DiscoveryOperations discoveryOperations;
    @Mock private DiscoveryAdapterFactory adapterFactory;
    @Mock private DiscoveryAdapter adapter;
    @Mock private ImagePauseOnScrollListener imagePauseOnScrollListener;
    @Mock private Navigator navigator;
    @Mock private RecommendationBucket recommendationBucketOne;
    @Mock private FeatureFlags featureFlags;
    @Mock private ChartsPresenter chartsPresenter;
    private EventBus eventBus = new TestEventBus();

    private DiscoveryPresenter presenter;

    @Before
    public void setUp() {
        this.presenter = new DiscoveryPresenter(
                swipeRefreshAttacher,
                discoveryOperations,
                adapterFactory,
                imagePauseOnScrollListener,
                navigator,
                featureFlags,
                eventBus);

        when(recommendationBucketOne.getSeedTrackUrn()).thenReturn(SEED_TRACK_URN);
        when(recommendationBucketOne.getRecommendations()).thenReturn(Collections.singletonList(RECOMMENDATION));
        when(featureFlags.isEnabled(Flag.DISCOVERY_RECOMMENDATIONS)).thenReturn(true);
        when(adapterFactory.create(any(Screen.class))).thenReturn(adapter);
    }

    @Test
    public void tagSelectedOpensPlaylistDiscoveryActivity() {
        final String playListTag = "playListTag";
        final Context context = context();

        presenter.onTagSelected(context, playListTag);

        verify(navigator).openPlaylistDiscoveryTag(context, playListTag);
    }
}
