package com.soundcloud.android.discovery.recommendations;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class RecommendationRendererTest extends AndroidUnitTest {

    private static final ApiTrack SEED_TRACK = ModelFixtures.create(ApiTrack.class);
    private List<Recommendation> recommendations;
    private int trackPosition;
    private int goTrackPosition;

    private RecommendationRenderer renderer;

    @Mock private ImageOperations imageOperations;
    @Mock private TrackItemMenuPresenter trackItemMenuPresenter;
    @Mock private RecommendationsAdapter adapter;
    @Mock private Navigator navigator;
    @Mock private TrackRecommendationListener listener;
    @Mock private FeatureFlags flags;
    private View itemView;
    private TrackItem recommendedTrack;

    @Before
    public void setUp() {
        final LayoutInflater layoutInflater = LayoutInflater.from(activity());
        itemView = layoutInflater.inflate(R.layout.recommendation_item, new FrameLayout(context()), false);
        renderer = new RecommendationRenderer(listener, flags, imageOperations, trackItemMenuPresenter, navigator);
        final Recommendation recommendation = RecommendationsFixtures.createNonHighTierRecommendation(SEED_TRACK.getUrn());
        recommendedTrack = recommendation.getTrack();
        final Recommendation goRecommendation = RecommendationsFixtures.createHighTierRecommendation(SEED_TRACK.getUrn());
        recommendations = Lists.newArrayList(recommendation, goRecommendation);
        trackPosition = recommendations.indexOf(recommendation);
        goTrackPosition = recommendations.indexOf(goRecommendation);
    }

    @Test
    public void shouldBindCreatorNameToView() {
        renderer.bindItemView(trackPosition, itemView, recommendations);

        Assertions.assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendation_artist))
                  .containsText(recommendedTrack.getCreatorName());
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(trackPosition, itemView, recommendations);

        Assertions.assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendation_title))
                  .containsText(recommendedTrack.getTitle());
    }

    @Test
    public void shouldBindOverflowMenuToView() {
        renderer.bindItemView(trackPosition, itemView, recommendations);
        final ImageView overflowButton = (ImageView) itemView.findViewById(R.id.overflow_button);

        overflowButton.performClick();

        verify(trackItemMenuPresenter).show((FragmentActivity) overflowButton.getContext(),
                                            overflowButton,
                                            recommendedTrack,
                                            0);
    }

    @Test
    public void shouldBindArtworkToView() {
        renderer.bindItemView(trackPosition, itemView, recommendations);

        verify(imageOperations).displayInAdapterView(
                recommendedTrack,
                ApiImageSize.getFullImageSize(itemView.getResources()),
                (ImageView) itemView.findViewById(R.id.recommendation_artwork));
    }

    @Test
    public void shouldNotSetGoIndicatorSelectedIfMidTierFlagIsDisabled() {
        renderer.bindItemView(goTrackPosition, itemView, recommendations);
        View goIndicator = ButterKnife.findById(itemView, R.id.go_indicator);
        assertThat(goIndicator.isSelected()).isFalse();
    }

    @Test
    public void shouldSetGoIndicatorSelectedIfMidTierFlagIsEnabled() {
        when(flags.isEnabled(Flag.MID_TIER_ROLLOUT)).thenReturn(true);

        renderer.bindItemView(goTrackPosition, itemView, recommendations);

        View goIndicator = ButterKnife.findById(itemView, R.id.go_indicator);
        assertThat(goIndicator.isSelected()).isTrue();
    }

    @Test
    public void shouldBindGoIndicatorForHighTierTracks() {
        renderer.bindItemView(goTrackPosition, itemView, recommendations);
        assertThat(ButterKnife.findById(itemView, R.id.go_indicator).getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void shouldNotBindGoIndicatorForNonHighTierTracks() {
        renderer.bindItemView(trackPosition, itemView, recommendations);
        assertThat(ButterKnife.findById(itemView, R.id.go_indicator).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void tappingOnRecommendationShouldPropagateEvent() {
        renderer.bindItemView(trackPosition, itemView, recommendations);
        itemView.performClick();
        verify(listener).onTrackClicked(SEED_TRACK.getUrn(), recommendedTrack.getUrn());
    }


    @Test
    public void tappingOnTrackArtistShouldNavigateToUserProfile() {
        renderer.bindItemView(trackPosition, itemView, recommendations);

        View artistName = itemView.findViewById(R.id.recommendation_artist);
        artistName.performClick();
        verify(navigator).legacyOpenProfile(artistName.getContext(), recommendedTrack.getCreatorUrn());
    }
}
