package com.soundcloud.android.image;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.util.AttributeSet;
import android.widget.ImageView;

public class StyledImageViewTest extends AndroidUnitTest {
    private static final Optional<String> IMAGE_URL_TEMPLATE = Optional.of("https://i1.sndcdn.com/artworks-c2Nb1yecOU1z-0-{size}.jpg");
    private static final String CACHE_KEY = "cacheKey";

    @Mock private AttributeSet attributeSet;
    @Mock private ImageOperations imageOperations;

    private StyledImageView styledImageView;
    private ImageView circularArtwork;
    private ImageView squareArtwork;
    private ImageView stationsIndicator;

    @Before
    public void setUp() throws Exception {
        styledImageView = new StyledImageView(context(), attributeSet);
        circularArtwork = (ImageView) styledImageView.findViewById(R.id.circular_artwork);
        squareArtwork = (ImageView) styledImageView.findViewById(R.id.square_artwork);
        stationsIndicator = (ImageView) styledImageView.findViewById(R.id.station_indicator);
    }

    @Test
    public void rendersCircularWithPlaceholderForCircularImageTypes() {
        styledImageView.showWithPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.CIRCULAR), CACHE_KEY, imageOperations);

        verify(imageOperations).displayCircularWithPlaceholder(CACHE_KEY, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), circularArtwork);
        assertThat(circularArtwork).isVisible();
        assertThat(squareArtwork).isNotVisible();
        assertThat(stationsIndicator).isNotVisible();
    }

    @Test
    public void rendersSquareWithPlaceholderForSquareImageTypes() {
        styledImageView.showWithPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.SQUARE), CACHE_KEY, imageOperations);

        verify(imageOperations).displayWithPlaceholder(CACHE_KEY, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isNotVisible();
    }

    @Test
    public void rendersSquareWithPlaceholderAndStationsOverlayForStationImageTypes() {
        styledImageView.showWithPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.STATION), CACHE_KEY, imageOperations);

        verify(imageOperations).displayWithPlaceholder(CACHE_KEY, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isVisible();
    }

    @Test
    public void rendersSquareWithPlaceholderByDefaultForAbsentImageType() {
        styledImageView.showWithPlaceholder(IMAGE_URL_TEMPLATE, Optional.absent(), CACHE_KEY, imageOperations);

        verify(imageOperations).displayWithPlaceholder(CACHE_KEY, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isNotVisible();
    }

    @Test
    public void rendersCircularWithoutPlaceholderForCircularImageTypes() {
        styledImageView.showWithoutPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.CIRCULAR), CACHE_KEY, imageOperations);

        verify(imageOperations).displayCircularInAdapterView(CACHE_KEY, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), circularArtwork);
        assertThat(circularArtwork).isVisible();
        assertThat(squareArtwork).isNotVisible();
        assertThat(stationsIndicator).isNotVisible();
    }

    @Test
    public void rendersSquareWithoutPlaceholderForSquareImageTypes() {
        styledImageView.showWithoutPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.SQUARE), CACHE_KEY, imageOperations);

        verify(imageOperations).displayInAdapterView(CACHE_KEY, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isNotVisible();
    }

    @Test
    public void rendersSquareWithoutPlaceholderAndStationsOverlayForStationImageTypes() {
        styledImageView.showWithoutPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.STATION), CACHE_KEY, imageOperations);

        verify(imageOperations).displayInAdapterView(CACHE_KEY, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isVisible();
    }

    @Test
    public void rendersSquareWithoutPlaceholderByDefaultForAbsentImageType() {
        styledImageView.showWithoutPlaceholder(IMAGE_URL_TEMPLATE, Optional.absent(), CACHE_KEY, imageOperations);

        verify(imageOperations).displayInAdapterView(CACHE_KEY, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isNotVisible();
    }
}
