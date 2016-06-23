package com.soundcloud.android.discovery;


import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import butterknife.ButterKnife;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class RecommendationRendererTest extends AndroidUnitTest {

    private static final int QUERY_POSITION = 99;
    private static final ApiTrack SEED_TRACK = ModelFixtures.create(ApiTrack.class);
    private static final TrackItem RECOMMENDED_TRACK = TrackItem.from(ModelFixtures.create(ApiTrack.class));
    private static final Recommendation RECOMMENDATION = new Recommendation(RECOMMENDED_TRACK, SEED_TRACK.getUrn(), false, QUERY_POSITION, Urn.NOT_SET);
    private static final List<Recommendation> RECOMMENDATIONS = Collections.singletonList(RECOMMENDATION);
    private static final int TRACK_POSITION = RECOMMENDATIONS.indexOf(RECOMMENDATION);

    private RecommendationRenderer renderer;

    @Mock private ImageOperations imageOperations;
    @Mock private TrackItemMenuPresenter trackItemMenuPresenter;
    @Mock private RecommendationsAdapter adapter;
    @Mock private Navigator navigator;
    @Mock private TrackRecommendationListener listener;
    private View itemView;

    @Before
    public void setUp() {
        final LayoutInflater layoutInflater = LayoutInflater.from(fragmentActivity());
        itemView = layoutInflater.inflate(R.layout.recommendation_item, new FrameLayout(context()), false);
        renderer = new RecommendationRenderer(listener, imageOperations, trackItemMenuPresenter, navigator);
    }

    @Test
    public void shouldBindCreatorNameToView() {
        renderer.bindItemView(TRACK_POSITION, itemView, RECOMMENDATIONS);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendation_artist)).containsText(RECOMMENDED_TRACK.getCreatorName());
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(TRACK_POSITION, itemView, RECOMMENDATIONS);

        assertThat(ButterKnife.<TextView>findById(itemView, R.id.recommendation_title)).containsText(RECOMMENDED_TRACK.getTitle());
    }

    @Test
    public void shouldBindOverflowMenuToView() {
        renderer.bindItemView(TRACK_POSITION, itemView, RECOMMENDATIONS);
        final ImageView overflowButton = (ImageView) itemView.findViewById(R.id.overflow_button);

        overflowButton.performClick();

        verify(trackItemMenuPresenter).show((FragmentActivity) overflowButton.getContext(), overflowButton, RECOMMENDED_TRACK, 0);
    }

    @Test
    public void shouldBindArtworkToView() {
        renderer.bindItemView(TRACK_POSITION, itemView, RECOMMENDATIONS);

        verify(imageOperations).displayInAdapterView(
                RECOMMENDED_TRACK,
                ApiImageSize.getFullImageSize(itemView.getResources()),
                (ImageView) itemView.findViewById(R.id.recommendation_artwork));
    }

    @Test
    public void tappingOnRecommendationShouldPropagateEvent() {
        renderer.bindItemView(TRACK_POSITION, itemView, RECOMMENDATIONS);
        itemView.performClick();
        verify(listener).onTrackClicked(SEED_TRACK.getUrn(), RECOMMENDED_TRACK.getUrn());
    }


    @Test
    public void tappingOnTrackArtistShouldNavigateToUserProfile() {
        renderer.bindItemView(TRACK_POSITION, itemView, RECOMMENDATIONS);

        View artistName = itemView.findViewById(R.id.recommendation_artist);
        artistName.performClick();
        verify(navigator).openProfile(artistName.getContext(), RECOMMENDED_TRACK.getCreatorUrn());
    }
}
