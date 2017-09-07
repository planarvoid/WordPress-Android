package com.soundcloud.android.collection;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CollectionPreviewViewTest extends AndroidUnitTest {

    private final int numThumbnails = 3;
    private final int expectedNumHolderViews = numThumbnails;

    @Mock ImageOperations imageOperations;
    @Mock Drawable previewDrawable;

    private CollectionPreviewView view;
    private ViewGroup holder;
    @Mock private ImageResource track1;
    @Mock private ImageResource track2;
    @Mock private ImageResource track3;

    @Before
    public void setUp() throws Exception {
        view = new CollectionPreviewView(context(), (Drawable) null);
        holder = (ViewGroup) view.findViewById(R.id.thumbnail_container);
        when(track1.getUrn()).thenReturn(Urn.forTrack(1));
        when(track2.getUrn()).thenReturn(Urn.forTrack(2));
        when(track3.getUrn()).thenReturn(Urn.forTrack(3));
        when(track1.getImageUrlTemplate()).thenReturn(Optional.of(("1")));
        when(track2.getImageUrlTemplate()).thenReturn(Optional.of(("2")));
        when(track3.getImageUrlTemplate()).thenReturn(Optional.of(("3")));
    }

    @Test
    public void bindViewAddsMaxThumbnailViewsIfOverMax() {
        final List<ImageResource> entities = Arrays.asList(track1, track2, track3, mock(ImageResource.class));

        view.refreshThumbnails(imageOperations, entities, numThumbnails);
        verify(imageOperations).displayWithPlaceholder(eq(track1.getUrn()), any(), any(ApiImageSize.class), eq(getImagePreview(0)));
        verify(imageOperations).displayWithPlaceholder(eq(track2.getUrn()), any(), any(ApiImageSize.class), eq(getImagePreview(1)));
        verify(imageOperations).displayWithPlaceholder(eq(track3.getUrn()), any(), any(ApiImageSize.class), eq(getImagePreview(2)));
        verifyNoMoreInteractions(imageOperations);
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void bindViewAddsMaxImageViews() {
        final List<ImageResource> entities = Arrays.asList(track1, track2, track3);

        view.refreshThumbnails(imageOperations, entities, numThumbnails);
        verify(imageOperations).displayWithPlaceholder(eq(track1.getUrn()), any(), any(ApiImageSize.class), eq(getImagePreview(0)));
        verify(imageOperations).displayWithPlaceholder(eq(track2.getUrn()), any(), any(ApiImageSize.class), eq(getImagePreview(1)));
        verify(imageOperations).displayWithPlaceholder(eq(track3.getUrn()), any(), any(ApiImageSize.class), eq(getImagePreview(2)));
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void bindViewAddsArtworkThumbnailsAtTheEnd() {
        final List<ImageResource> entities = Arrays.asList(track1, track2);

        view.refreshThumbnails(imageOperations, entities, numThumbnails);

        verify(imageOperations, never()).displayWithPlaceholder(eq(track3.getUrn()), any(), any(ApiImageSize.class), eq(getImagePreview(0)));
        verify(imageOperations).displayWithPlaceholder(eq(track1.getUrn()), any(), any(ApiImageSize.class), eq(getImagePreview(1)));
        verify(imageOperations).displayWithPlaceholder(eq(track2.getUrn()), any(), any(ApiImageSize.class), eq(getImagePreview(2)));
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void bindViewAddsEmptyThumbnailsIfNoEntities() {
        final List<ImageResource> entities = Collections.emptyList();

        view.refreshThumbnails(imageOperations, entities, numThumbnails);
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
        verifyZeroInteractions(imageOperations);
    }

    @Test
    public void updatingTheCollectionShowsCorrectNumberOfThumbnails() {
        final List<ImageResource> original = Arrays.asList(
                track1, track2, track3
        );
        final List<ImageResource> update = Collections.singletonList(track3);

        view.refreshThumbnails(imageOperations, original, numThumbnails);
        view.refreshThumbnails(imageOperations, update, numThumbnails);

        verify(imageOperations).displayWithPlaceholder(eq(update.get(0).getUrn()), any(), any(ApiImageSize.class), eq(getImagePreview(2)));
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void showsPreviewOverlayOnEntitiesPreviewsOnly() {
        final List<ImageResource> entities = Arrays.asList(track1, track2);
        view = new CollectionPreviewView(context(), previewDrawable);
        holder = (ViewGroup) view.findViewById(R.id.thumbnail_container);

        view.refreshThumbnails(imageOperations, entities, numThumbnails);

        assertThat(getPreviewOverlay(2).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getPreviewOverlay(1).getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(getPreviewOverlay(0).getVisibility()).isEqualTo(View.GONE);
    }

    private ImageView getImagePreview(int index) {
        return (ImageView) holder.getChildAt(index).findViewById(R.id.preview_artwork);
    }

    private ImageView getPreviewOverlay(int index) {
        return (ImageView) holder.getChildAt(index).findViewById(R.id.artwork_overlay);
    }
}
