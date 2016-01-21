package com.soundcloud.android.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CollectionPreviewViewTest extends AndroidUnitTest {

    private final int numThumbnails = 3;
    private final int expectedNumHolderViews = numThumbnails;

    @Mock ImageOperations imageOperations;
    private CollectionPreviewView view;
    private ViewGroup holder;

    @Before
    public void setUp() throws Exception {
        view = new CollectionPreviewView(context(), imageOperations);
        holder = (ViewGroup) view.findViewById(R.id.thumbnail_container);
    }

    @Test
    public void bindViewAddsMaxThumbnailViewsIfOverMax() {
        final Urn track1 = Urn.forTrack(1L);
        final Urn track2 = Urn.forTrack(2L);
        final Urn track3 = Urn.forTrack(3L);
        final List<Urn> entities = Arrays.asList(track1, track2, track3, Urn.forTrack(4L));

        view.refreshThumbnails(entities, numThumbnails);
        verify(imageOperations).displayWithPlaceholder(eq(track1), any(ApiImageSize.class), eq((ImageView) holder.getChildAt(0)));
        verify(imageOperations).displayWithPlaceholder(eq(track2), any(ApiImageSize.class), eq((ImageView) holder.getChildAt(1)));
        verify(imageOperations).displayWithPlaceholder(eq(track3), any(ApiImageSize.class), eq((ImageView) holder.getChildAt(2)));
        verifyNoMoreInteractions(imageOperations);
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void bindViewAddsMaxImageViews() {
        final Urn track1 = Urn.forTrack(1L);
        final Urn track2 = Urn.forTrack(2L);
        final Urn track3 = Urn.forTrack(3L);
        final List<Urn> entities = Arrays.asList(track1, track2, track3);

        view.refreshThumbnails(entities, numThumbnails);
        verify(imageOperations).displayWithPlaceholder(eq(track1), any(ApiImageSize.class), eq((ImageView) holder.getChildAt(0)));
        verify(imageOperations).displayWithPlaceholder(eq(track2), any(ApiImageSize.class), eq((ImageView) holder.getChildAt(1)));
        verify(imageOperations).displayWithPlaceholder(eq(track3), any(ApiImageSize.class), eq((ImageView) holder.getChildAt(2)));
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void bindViewAddsArtworkThumbnailsAtTheEnd() {
        final Urn track1 = Urn.forTrack(1L);
        final Urn track2 = Urn.forTrack(2L);
        final List<Urn> entities = Arrays.asList(track1, track2);

        view.refreshThumbnails(entities, numThumbnails);

        verify(imageOperations, never()).displayWithPlaceholder(any(Urn.class), any(ApiImageSize.class), eq((ImageView) holder.getChildAt(0)));
        verify(imageOperations).displayWithPlaceholder(eq(track1), any(ApiImageSize.class), eq((ImageView) holder.getChildAt(1)));
        verify(imageOperations).displayWithPlaceholder(eq(track2), any(ApiImageSize.class), eq((ImageView) holder.getChildAt(2)));
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void bindViewAddsEmptyThumbnailsIfNoEntities() {
        final List<Urn> entities = Collections.emptyList();

        view.refreshThumbnails(entities, numThumbnails);
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
        verifyZeroInteractions(imageOperations);
    }

    @Test
    public void updatingTheCollectionShowsCorrectNumberOfThumbnails() {
        final List<Urn> original = Arrays.asList(Urn.forTrack(1L), Urn.forTrack(2L), Urn.forTrack(3L));
        final List<Urn> update = Collections.singletonList(Urn.forTrack(4L));

        view.refreshThumbnails(original, numThumbnails);
        view.refreshThumbnails(update, numThumbnails);

        verify(imageOperations).displayWithPlaceholder(eq(update.get(0)), any(ApiImageSize.class), eq((ImageView) holder.getChildAt(2)));
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

}
