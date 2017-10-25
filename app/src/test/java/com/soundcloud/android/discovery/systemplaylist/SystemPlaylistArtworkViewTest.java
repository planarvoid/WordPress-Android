package com.soundcloud.android.discovery.systemplaylist;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SystemPlaylistArtworkViewTest extends AndroidUnitTest {

    private SystemPlaylistArtworkView view;
    private ViewFlipper artworkContainer;
    @Mock private ImageOperations imageOperations;


    @Before
    public void setUp() throws Exception {
        view = new SystemPlaylistArtworkView(context());
        artworkContainer = view.findViewById(R.id.artwork_animator);
    }

    @Test
    public void bindViewRemoveAllViewsFromTheContainer() {
        final List<ImageResource> entities = Collections.emptyList();
        artworkContainer.addView(new View(context()));
        assertThat(artworkContainer.getChildCount()).isGreaterThan(0);
        view.bindWithAnimation(imageOperations, entities);

        assertThat(artworkContainer.getChildCount()).isEqualTo(0);
    }

    @Test
    public void bindViewCreatesImageView() {
        final SimpleImageResource track1 = SimpleImageResource.create(Urn.forTrack(1L), Optional.absent());
        final SimpleImageResource track2 = SimpleImageResource.create(Urn.forTrack(2L), Optional.absent());
        final SimpleImageResource track3 = SimpleImageResource.create(Urn.forTrack(3L), Optional.absent());

        final List<ImageResource> entities = Arrays.asList(track1, track2, track3);
        final Single<Bitmap> empty = Single.never();
        when(imageOperations.displayWithPlaceholderObservable(any(Urn.class), eq(Optional.absent()), any(ApiImageSize.class), any(ImageView.class))).thenReturn(empty);

        view.bindWithAnimation(imageOperations, entities);

        assertThat(artworkContainer.getChildCount()).isEqualTo(entities.size());
    }

    @Test
    public void bindViewStartsAnimatingWhenAllImagesHaveLoaded() {
        final ImageResource track1 = mock(ImageResource.class);
        when(track1.getUrn()).thenReturn(Urn.forTrack(1L));
        when(track1.getImageUrlTemplate()).thenReturn(Optional.absent());
        final List<ImageResource> entities = Collections.singletonList(track1);

        final Single<Bitmap> observable = Single.just(mock(Bitmap.class));

        when(imageOperations.displayWithPlaceholderObservable(any(Urn.class), eq(Optional.absent()), any(ApiImageSize.class), any(ImageView.class))).thenReturn(observable);

        view.bindWithAnimation(imageOperations, entities);

        assertThat(artworkContainer.isFlipping()).isTrue();
    }
}
