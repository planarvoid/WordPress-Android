package com.soundcloud.android.discovery.newforyou;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NewForYouArtworkViewTest extends AndroidUnitTest {

    private NewForYouArtworkView view;
    private ViewFlipper artworkContainer;
    @Mock private ImageOperations imageOperations;


    @Before
    public void setUp() throws Exception {
        view = new NewForYouArtworkView(context());
        artworkContainer = (ViewFlipper) view.findViewById(R.id.artwork_animator);
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
        final ImageResource track1 = mock(ImageResource.class);
        final ImageResource track2 = mock(ImageResource.class);
        final ImageResource track3 = mock(ImageResource.class);
        final List<ImageResource> entities = Arrays.asList(track1, track2, track3);
        final Observable<Bitmap> empty = Observable.empty();
        when(imageOperations.displayWithPlaceholderObservable(any(ImageResource.class), any(ApiImageSize.class), any(ImageView.class))).thenReturn(empty);

        view.bindWithAnimation(imageOperations, entities);

        assertThat(artworkContainer.getChildCount()).isEqualTo(entities.size());
    }

    @Test
    public void bindViewStartsAnimatingWhenAllImagesHaveLoaded() {
        final ImageResource track1 = mock(ImageResource.class);
        final List<ImageResource> entities = Collections.singletonList(track1);

        final Observable<Bitmap> observable = Observable.just(mock(Bitmap.class));

        when(imageOperations.displayWithPlaceholderObservable(any(ImageResource.class), any(ApiImageSize.class), any(ImageView.class))).thenReturn(observable);

        view.bindWithAnimation(imageOperations, entities);

        assertThat(artworkContainer.isFlipping()).isTrue();
    }
}
