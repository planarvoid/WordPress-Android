package com.soundcloud.android.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.testsupport.AndroidUnitTest;
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

    @Before
    public void setUp() throws Exception {
        view = new CollectionPreviewView(context(), imageOperations, null);
        holder = (ViewGroup) view.findViewById(R.id.thumbnail_container);
    }

    @Test
    public void bindViewAddsMaxThumbnailViewsIfOverMax() {
        final ImageResource track1 = mock(ImageResource.class);
        final ImageResource track2 = mock(ImageResource.class);
        final ImageResource track3 = mock(ImageResource.class);
        final List<ImageResource> entities = Arrays.asList(track1, track2, track3, mock(ImageResource.class));

        view.refreshThumbnails(entities, numThumbnails);
        verify(imageOperations).displayWithPlaceholder(same(track1),
                                                       any(ApiImageSize.class),
                                                       eq(getImagePreview(0)));
        verify(imageOperations).displayWithPlaceholder(same(track2),
                                                       any(ApiImageSize.class),
                                                       eq(getImagePreview(1)));
        verify(imageOperations).displayWithPlaceholder(same(track3),
                                                       any(ApiImageSize.class),
                                                       eq(getImagePreview(2)));
        verifyNoMoreInteractions(imageOperations);
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void bindViewAddsMaxImageViews() {
        final ImageResource track1 = mock(ImageResource.class);
        final ImageResource track2 = mock(ImageResource.class);
        final ImageResource track3 = mock(ImageResource.class);
        final List<ImageResource> entities = Arrays.asList(track1, track2, track3);

        view.refreshThumbnails(entities, numThumbnails);
        verify(imageOperations).displayWithPlaceholder(same(track1),
                                                       any(ApiImageSize.class),
                                                       eq(getImagePreview(0)));
        verify(imageOperations).displayWithPlaceholder(same(track2),
                                                       any(ApiImageSize.class),
                                                       eq(getImagePreview(1)));
        verify(imageOperations).displayWithPlaceholder(same(track3),
                                                       any(ApiImageSize.class),
                                                       eq(getImagePreview(2)));
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void bindViewAddsArtworkThumbnailsAtTheEnd() {
        final ImageResource track1 = mock(ImageResource.class);
        final ImageResource track2 = mock(ImageResource.class);
        final List<ImageResource> entities = Arrays.asList(track1, track2);

        view.refreshThumbnails(entities, numThumbnails);

        verify(imageOperations, never()).displayWithPlaceholder(any(ImageResource.class),
                                                                any(ApiImageSize.class),
                                                                eq(getImagePreview(0)));
        verify(imageOperations).displayWithPlaceholder(same(track1),
                                                       any(ApiImageSize.class),
                                                       eq(getImagePreview(1)));
        verify(imageOperations).displayWithPlaceholder(same(track2),
                                                       any(ApiImageSize.class),
                                                       eq(getImagePreview(2)));
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void bindViewAddsEmptyThumbnailsIfNoEntities() {
        final List<ImageResource> entities = Collections.emptyList();

        view.refreshThumbnails(entities, numThumbnails);
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
        verifyZeroInteractions(imageOperations);
    }

    @Test
    public void updatingTheCollectionShowsCorrectNumberOfThumbnails() {
        final List<ImageResource> original = Arrays.asList(
                mock(ImageResource.class), mock(ImageResource.class), mock(ImageResource.class)
        );
        final List<ImageResource> update = Collections.singletonList(mock(ImageResource.class));

        view.refreshThumbnails(original, numThumbnails);
        view.refreshThumbnails(update, numThumbnails);

        verify(imageOperations).displayWithPlaceholder(same(update.get(0)),
                                                       any(ApiImageSize.class),
                                                       eq(getImagePreview(2)));
        assertThat(holder.getChildCount()).isEqualTo(expectedNumHolderViews);
    }

    @Test
    public void showsPreviewOverlayOnEntitiesPreviewsOnly() {
        final List<ImageResource> entities = Arrays.asList(
                mock(ImageResource.class), mock(ImageResource.class));
        view = new CollectionPreviewView(context(), imageOperations, previewDrawable);
        holder = (ViewGroup) view.findViewById(R.id.thumbnail_container);

        view.refreshThumbnails(entities, numThumbnails);

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
