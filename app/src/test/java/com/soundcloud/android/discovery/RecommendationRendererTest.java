package com.soundcloud.android.discovery;


import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ApiImageSize;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecommendationRendererTest extends AndroidUnitTest {
    private final static ApiTrack SEED_TRACK = ModelFixtures.create(ApiTrack.class);
    private final static TrackItem RECOMMENDED_TRACK = TrackItem.from(ModelFixtures.create(ApiTrack.class));
    private final static Recommendation RECOMMENDATION = new Recommendation(RECOMMENDED_TRACK, SEED_TRACK.getUrn(), false);
    private final static List<Recommendation> RECOMMENDATIONS = Collections.singletonList(RECOMMENDATION);
    private final static List<Urn> TRACKS_LIST = Arrays.asList(SEED_TRACK.getUrn(), RECOMMENDED_TRACK.getUrn());

    private RecommendationRenderer renderer;

    @Mock private ImageOperations imageOperations;
    @Mock private TrackItemMenuPresenter trackItemMenuPresenter;
    @Mock private RecommendationsAdapter adapter;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private Navigator navigator;
    private Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider = TestSubscribers.expandPlayerSubscriber();

    private View itemView;

    @Before
    public void setUp() {
        final LayoutInflater layoutInflater = LayoutInflater.from(fragmentActivity());

        itemView = layoutInflater.inflate(R.layout.recommendation_item, new FrameLayout(context()), false);
        renderer = new RecommendationRenderer(Screen.RECOMMENDATIONS_MAIN, imageOperations, trackItemMenuPresenter, playbackInitiator, expandPlayerSubscriberProvider, navigator);
        when(playbackInitiator.playTracks(anyListOf(Urn.class), any(Integer.class), any(PlaySessionSource.class))).thenReturn(Observable.just(PlaybackResult.success()));
    }

    @Test
    public void shouldBindCreatorNameToView() {
        renderer.bindItemView(0, itemView, RECOMMENDATIONS);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendation_artist)).containsText(RECOMMENDED_TRACK.getCreatorName());
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(0, itemView, RECOMMENDATIONS);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendation_title)).containsText(RECOMMENDED_TRACK.getTitle());
    }

    @Test
    public void shouldBindOverflowMenuToView() {
        renderer.bindItemView(0, itemView, RECOMMENDATIONS);
        final ImageView overflowButton = (ImageView) itemView.findViewById(R.id.overflow_button);

        overflowButton.performClick();

        verify(trackItemMenuPresenter).show((FragmentActivity) overflowButton.getContext(), overflowButton, RECOMMENDED_TRACK, 0);
    }

    @Test
    public void shouldBindArtworkToView() {
        renderer.bindItemView(0, itemView, RECOMMENDATIONS);

        verify(imageOperations).displayInAdapterView(
                RECOMMENDED_TRACK,
                ApiImageSize.getFullImageSize(itemView.getResources()),
                (ImageView) itemView.findViewById(R.id.recommendation_artwork));
    }

    @Test
    public void tappingOnRecommendationShouldStartPlayback() {
        renderer.bindItemView(0, itemView, RECOMMENDATIONS);

        itemView.performClick();
        verify(playbackInitiator).playTracks(TRACKS_LIST, 1, new PlaySessionSource(Screen.RECOMMENDATIONS_MAIN));
    }


    @Test
    public void tappingOnTrackArtistShouldNavigateToUserProfile() {
        renderer.bindItemView(0, itemView, RECOMMENDATIONS);

        View artistName = itemView.findViewById(R.id.recommendation_artist);
        artistName.performClick();
        verify(navigator).openProfile(artistName.getContext(), RECOMMENDED_TRACK.getCreatorUrn());
    }
}
