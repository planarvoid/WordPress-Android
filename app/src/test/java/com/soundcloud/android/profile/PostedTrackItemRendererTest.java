package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.GoOnboardingTooltipExperiment;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.offline.OfflineSettingsOperations;
import com.soundcloud.android.stream.RepostedProperties;
import com.soundcloud.android.stream.StreamEntity;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemView;
import com.soundcloud.android.tracks.TrackStatsDisplayPolicy;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.widget.ImageView;

import java.util.Date;
import java.util.Locale;

public class PostedTrackItemRendererTest extends AndroidUnitTest {

    @Mock TrackItemView trackItemView;
    @Mock ImageView imageView;
    @Mock View itemView;
    @Mock EventBus eventBus;
    @Mock ScreenProvider screenProvider;
    @Mock Navigator navigator;
    @Mock FeatureOperations featureOperations;
    @Mock TrackItemView.Factory factory;
    @Mock OfflineSettingsOperations offlineSettingsOperations;
    @Mock NetworkConnectionHelper networkConnectionHelper;
    @Mock GoOnboardingTooltipExperiment goOnboardingTooltipExperiment;
    @Mock IntroductoryOverlayPresenter introductoryOverlayPresenter;
    @Mock TrackStatsDisplayPolicy trackStatsDisplayPolicy;

    private TrackItem trackItem;
    private PostedTrackItemRenderer renderer;

    private final CondensedNumberFormatter numberFormatter = CondensedNumberFormatter.create(Locale.US, resources());
    private Track track;

    @Before
    public void setUp() throws Exception {
        track = ModelFixtures.trackBuilder()
                             .title("title")
                             .creatorName("creator")
                             .snippetDuration(227000L)
                             .fullDuration(227000L)
                             .snipped(true)
                             .urn(Urn.forTrack(123))
                             .playCount(870).build();
        trackItem = ModelFixtures.trackItem(track);

        when(trackItemView.getImage()).thenReturn(imageView);
        when(trackItemView.getResources()).thenReturn(resources());
        when(imageView.getContext()).thenReturn(context());
        when(itemView.getContext()).thenReturn(context());
        when(itemView.getTag()).thenReturn(trackItemView);
        when(trackStatsDisplayPolicy.displayPlaysCount(any())).thenReturn(true);

        renderer = new PostedTrackItemRenderer(mock(ImageOperations.class),
                                               numberFormatter,
                                               null,
                                               eventBus,
                                               screenProvider,
                                               navigator,
                                               featureOperations,
                                               factory,
                                               offlineSettingsOperations,
                                               networkConnectionHelper,
                                               goOnboardingTooltipExperiment,
                                               lazyOf(introductoryOverlayPresenter),
                                               trackStatsDisplayPolicy);
    }

    @Test
    public void shouldBindReposterIfAny() {
        final String reposter = "reposter";
        final TrackItem reposterTrack = ModelFixtures.trackItem(track,
                                                                StreamEntity.builder(track.urn(), new Date())
                                                                            .repostedProperties(RepostedProperties.create(reposter, Urn.NOT_SET))
                                                                            .build());
        renderer.bindItemView(0, itemView, singletonList(reposterTrack));

        verify(trackItemView).showReposter(reposter);
    }

    @Test
    public void shouldNotBindReposterIfNone() {
        renderer.bindItemView(0, itemView, singletonList(trackItem));

        verify(trackItemView).hideReposter();
    }
}
