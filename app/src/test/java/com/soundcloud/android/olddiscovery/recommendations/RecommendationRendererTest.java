package com.soundcloud.android.olddiscovery.recommendations;

import static com.soundcloud.android.helpers.NavigationTargetMatcher.matchesNavigationTarget;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.properties.FeatureFlags;
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
    @Mock private TrackRecommendationListener listener;
    @Mock private FeatureFlags flags;
    @Mock private Navigator navigator;
    private View itemView;
    private TrackItem recommendedTrack;

    @Before
    public void setUp() {
        final LayoutInflater layoutInflater = LayoutInflater.from(activity());
        itemView = layoutInflater.inflate(R.layout.recommendation_item, new FrameLayout(context()), false);
        renderer = new RecommendationRenderer(listener, imageOperations, trackItemMenuPresenter, navigator);
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

        Assertions.assertThat(ButterKnife.findById(itemView, R.id.recommendation_artist))
                  .containsText(recommendedTrack.creatorName());
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(trackPosition, itemView, recommendations);

        Assertions.assertThat(ButterKnife.findById(itemView, R.id.recommendation_title))
                  .containsText(recommendedTrack.title());
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
        verify(navigator).navigateTo(argThat(matchesNavigationTarget(NavigationTarget.forProfile(recommendedTrack.creatorUrn()))));
    }

}
