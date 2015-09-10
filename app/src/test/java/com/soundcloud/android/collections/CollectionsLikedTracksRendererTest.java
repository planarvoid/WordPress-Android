package com.soundcloud.android.collections;

import static com.soundcloud.android.collections.CollectionsLikedTracksRenderer.EXTRA_HOLDER_VIEWS;
import static com.soundcloud.android.collections.CollectionsLikedTracksRenderer.MAX_LIKE_IMAGES;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

public class CollectionsLikedTracksRendererTest extends AndroidUnitTest {

    private CollectionsLikedTracksRenderer renderer;
    @Mock private Navigator navigator;
    @Mock private ImageOperations imageOperations;

    private View view;

    @Before
    public void setUp() throws Exception {
        renderer = new CollectionsLikedTracksRenderer(navigator, imageOperations);
        view = LayoutInflater.from(context()).inflate(R.layout.collection_liked_tracks_item, null);
    }

    @Test
    public void bindViewAddsMaxImageViewsIfOverMax() {
        final List<Urn> likes = Arrays.asList(Urn.forTrack(1L), Urn.forTrack(2L), Urn.forTrack(3L), Urn.forTrack(4L));

        renderer.bindItemView(0, view, Arrays.asList(CollectionsItem.fromLikes(likes)));

        assertThat(((ViewGroup)view.findViewById(R.id.holder)).getChildCount()).isEqualTo(MAX_LIKE_IMAGES + EXTRA_HOLDER_VIEWS);
    }

    @Test
    public void bindViewAddsMaxImageViews() {
        final List<Urn> likes = Arrays.asList(Urn.forTrack(1L), Urn.forTrack(2L), Urn.forTrack(3L));

        renderer.bindItemView(0, view, Arrays.asList(CollectionsItem.fromLikes(likes)));

        assertThat(((ViewGroup)view.findViewById(R.id.holder)).getChildCount()).isEqualTo(MAX_LIKE_IMAGES + EXTRA_HOLDER_VIEWS);
    }

    @Test
    public void bindViewRemovedExtraImageViews() {
        final List<Urn> likes = Arrays.asList(Urn.forTrack(1L), Urn.forTrack(2L), Urn.forTrack(3L));

        renderer.bindItemView(0, view, Arrays.asList(CollectionsItem.fromLikes(likes)));
        renderer.bindItemView(0, view, Arrays.asList(CollectionsItem.fromLikes(Arrays.asList(Urn.forTrack(1L)))));

        assertThat(((ViewGroup)view.findViewById(R.id.holder)).getChildCount()).isEqualTo(2);
    }
}
