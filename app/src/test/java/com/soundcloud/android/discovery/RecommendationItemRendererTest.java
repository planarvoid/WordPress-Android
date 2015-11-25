package com.soundcloud.android.discovery;

import static com.soundcloud.android.testsupport.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecommendationItemRendererTest extends AndroidUnitTest {

    private RecommendationItemRenderer renderer;

    @Mock private ImageOperations imagesOperations;

    private View itemView;
    private List<RecommendationItem> recommendationItems;

    @Before
    public void setUp() {
        PropertySet propertySet = PropertySet.from(
                RecommendationProperty.SEED_TRACK_URN.bind(Urn.forTrack(123)),
                RecommendationProperty.SEED_TRACK_TITLE.bind("seed_title"),
                RecommendationProperty.REASON.bind(RecommendationReason.LIKED),
                RecommendationProperty.RECOMMENDED_TRACKS_COUNT.bind(10),
                RecommendedTrackProperty.URN.bind(Urn.forTrack(1234)),
                RecommendedTrackProperty.TITLE.bind("recommendation_title"),
                RecommendedTrackProperty.USERNAME.bind("username")
        );
        RecommendationItem recommendationItem = new RecommendationItem(propertySet);
        final LayoutInflater layoutInflater = LayoutInflater.from(context());
        itemView = layoutInflater.inflate(R.layout.recommendation_item, new FrameLayout(context()), false);
        recommendationItems = new ArrayList(Collections.singletonList(recommendationItem));
        renderer = new RecommendationItemRenderer(context().getResources(), imagesOperations);
    }

    @Test
    public void shouldBindTitleToView() {
        renderer.bindItemView(0, itemView, recommendationItems);

        assertThat(textView(R.id.title)).containsText("recommendation_title");
    }

    @Test
    public void shouldBindReasonToView() {
        renderer.bindItemView(0, itemView, recommendationItems);

        assertThat(textView(R.id.reason)).containsText("Because you liked seed_title");
    }

    @Test
    public void shouldBindUsernameToView() {
        renderer.bindItemView(0, itemView, recommendationItems);

        assertThat(textView(R.id.username)).containsText("username");
    }

    @Test
    public void shouldBindViewAllToView() {
        renderer.bindItemView(0, itemView, recommendationItems);

        assertThat(textView(R.id.view_all)).containsText("VIEW ALL 10");
    }

    private TextView textView(int id) {
        return ((TextView) itemView.findViewById(id));
    }
}