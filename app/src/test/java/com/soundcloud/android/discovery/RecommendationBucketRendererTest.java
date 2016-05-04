package com.soundcloud.android.discovery;


import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class RecommendationBucketRendererTest extends AndroidUnitTest {
    private static final long SEED_ID = 1;
    private static final RecommendationReason REASON = RecommendationReason.LIKED;
    private final ApiTrack seedTrack = ModelFixtures.create(ApiTrack.class);
    private final TrackItem recommendedTrack = TrackItem.from(ModelFixtures.create(ApiTrack.class));

    private RecommendationBucketRenderer renderer;

    @Mock private ImageOperations imageOperations;

    private View itemView;
    private List<RecommendationBucket> recommendationBuckets;

    @Before
    public void setUp() {
        PropertySet propertySet = createSeed();

        final RecommendationBucket recommendationBucket = new RecommendationBucket(propertySet, Collections.singletonList(recommendedTrack));
        final LayoutInflater layoutInflater = LayoutInflater.from(context());

        itemView = layoutInflater.inflate(R.layout.recommendation_bucket, new FrameLayout(context()), false);
        recommendationBuckets = Collections.singletonList(recommendationBucket);
        renderer = new RecommendationBucketRenderer(context().getResources(), imageOperations);
    }

    @Test
    public void shouldBindHeadingToView() {
        renderer.bindItemView(0, itemView, recommendationBuckets);

        assertThat(textView(R.id.recommendations_header)).containsText(itemView.getResources().getString(R.string.recommendation_seeds_header));
    }

    @Test
    public void shouldBindReasonToView() {
        renderer.bindItemView(0, itemView, recommendationBuckets);

        assertThat(textView(R.id.reason)).containsText("Because you liked " + seedTrack.getTitle());
    }

    @Test
    public void shouldBindCreatorNameToView() {
        renderer.bindItemView(0, itemView, recommendationBuckets);

        assertThat(textView(R.id.recommendation_artist)).containsText(recommendedTrack.getCreatorName());
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(0, itemView, recommendationBuckets);

        assertThat(textView(R.id.recommendation_title)).containsText(recommendedTrack.getTitle());
    }

    @Test
    public void shouldBindArtworkToView() {
        renderer.bindItemView(0, itemView, recommendationBuckets);

        verify(imageOperations).displayInAdapterView(
                recommendedTrack,
                ApiImageSize.getFullImageSize(itemView.getResources()),
                (ImageView) itemView.findViewById(R.id.recommendation_artwork));
    }

    @Test
    public void shouldBindViewAllToView() {
        renderer.bindItemView(0, itemView, recommendationBuckets);

        assertThat(textView(R.id.view_all)).containsText(itemView.getResources().getString(R.string.recommendation_view_all));
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }

    private PropertySet createSeed() {
        return PropertySet.from(
                RecommendationProperty.SEED_TRACK_LOCAL_ID.bind(SEED_ID),
                RecommendationProperty.SEED_TRACK_URN.bind(seedTrack.getUrn()),
                RecommendationProperty.SEED_TRACK_TITLE.bind(seedTrack.getTitle()),
                RecommendationProperty.REASON.bind(REASON)
        );
    }
}
