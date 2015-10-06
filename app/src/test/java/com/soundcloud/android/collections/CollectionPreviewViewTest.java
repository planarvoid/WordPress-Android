package com.soundcloud.android.collections;

import static com.soundcloud.android.collections.CollectionPreviewView.EXTRA_HOLDER_VIEWS;
import static com.soundcloud.android.collections.CollectionPreviewView.MAX_IMAGES;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.ViewGroup;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CollectionPreviewViewTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;
    private CollectionPreviewView view;

    @Before
    public void setUp() throws Exception {
        view = new CollectionPreviewView(context(), imageOperations);
    }

    @Test
    public void bindViewAddsMaxImageViewsIfOverMax() {
        final List<Urn> entities = Arrays.asList(Urn.forTrack(1L), Urn.forTrack(2L), Urn.forTrack(3L), Urn.forTrack(4L));

        view.populateArtwork(entities);

        assertThat(((ViewGroup) view.findViewById(R.id.holder)).getChildCount()).isEqualTo(MAX_IMAGES + EXTRA_HOLDER_VIEWS);
    }

    @Test
    public void bindViewAddsMaxImageViews() {
        final List<Urn> entities = Arrays.asList(Urn.forTrack(1L), Urn.forTrack(2L), Urn.forTrack(3L));

        view.populateArtwork(entities);

        assertThat(((ViewGroup) view.findViewById(R.id.holder)).getChildCount()).isEqualTo(MAX_IMAGES + EXTRA_HOLDER_VIEWS);
    }

    @Test
    public void bindViewRemovedExtraImageViews() {
        final List<Urn> entities = Arrays.asList(Urn.forTrack(1L), Urn.forTrack(2L), Urn.forTrack(3L));
        final List<Urn> update = Collections.singletonList(Urn.forTrack(1L));

        view.populateArtwork(entities);
        view.populateArtwork(update);

        assertThat(((ViewGroup) view.findViewById(R.id.holder)).getChildCount()).isEqualTo(2);
    }

}
