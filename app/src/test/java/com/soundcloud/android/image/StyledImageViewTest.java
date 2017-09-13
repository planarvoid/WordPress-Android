package com.soundcloud.android.image;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.util.AttributeSet;
import android.widget.ImageView;

public class StyledImageViewTest extends AndroidUnitTest {
    private static final Optional<String> IMAGE_URL_TEMPLATE = Optional.of("https://i1.sndcdn.com/artworks-c2Nb1yecOU1z-0-{size}.jpg");
    private static final Urn URN = new Urn("soundcloud:tracks:123");

    @Mock private AttributeSet attributeSet;
    @Mock private ImageOperations imageOperations;

    private StyledImageView styledImageView;
    private ImageView circularArtwork;
    private ImageView squareArtwork;
    private ImageView stationsIndicator;

    @Before
    public void setUp() throws Exception {
        styledImageView = new StyledImageView(context(), attributeSet);
        circularArtwork = styledImageView.findViewById(R.id.circular_artwork);
        squareArtwork = styledImageView.findViewById(R.id.square_artwork);
        stationsIndicator = styledImageView.findViewById(R.id.station_indicator);
    }

    @Test
    public void rendersCircularWithPlaceholderForCircularImageTypes() {
        styledImageView.showWithPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.CIRCULAR), Optional.of(URN), imageOperations);

        verify(imageOperations).displayCircularWithPlaceholder(URN, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), circularArtwork);
        assertThat(circularArtwork).isVisible();
        assertThat(squareArtwork).isNotVisible();
        assertThat(stationsIndicator).isNotVisible();
    }

    @Test
    public void rendersSquareWithPlaceholderForSquareImageTypes() {
        styledImageView.showWithPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.SQUARE), Optional.of(URN), imageOperations);

        verify(imageOperations).displayWithPlaceholder(URN, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isNotVisible();
    }

    @Test
    public void rendersSquareWithPlaceholderAndStationsOverlayForStationImageTypes() {
        styledImageView.showWithPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.STATION), Optional.of(URN), imageOperations);

        verify(imageOperations).displayWithPlaceholder(URN, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isVisible();
    }

    @Test
    public void rendersSquareWithPlaceholderByDefaultForAbsentImageType() {
        styledImageView.showWithPlaceholder(IMAGE_URL_TEMPLATE, Optional.absent(), Optional.of(URN), imageOperations);

        verify(imageOperations).displayWithPlaceholder(URN, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isNotVisible();
    }

    @Test
    public void rendersCircularWithoutPlaceholderForCircularImageTypes() {
        styledImageView.showWithoutPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.CIRCULAR), URN, imageOperations);

        verify(imageOperations).displayInAdapterView(URN, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), circularArtwork, ImageOperations.DisplayType.CIRCULAR);
        assertThat(circularArtwork).isVisible();
        assertThat(squareArtwork).isNotVisible();
        assertThat(stationsIndicator).isNotVisible();
    }

    @Test
    public void rendersSquareWithoutPlaceholderForSquareImageTypes() {
        styledImageView.showWithoutPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.SQUARE), URN, imageOperations);

        verify(imageOperations).displayInAdapterView(URN, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork, ImageOperations.DisplayType.DEFAULT);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isNotVisible();
    }

    @Test
    public void rendersSquareWithoutPlaceholderAndStationsOverlayForStationImageTypes() {
        styledImageView.showWithoutPlaceholder(IMAGE_URL_TEMPLATE, Optional.of(ImageStyle.STATION), URN, imageOperations);

        verify(imageOperations).displayInAdapterView(URN, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork, ImageOperations.DisplayType.DEFAULT);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isVisible();
    }

    @Test
    public void rendersSquareWithoutPlaceholderByDefaultForAbsentImageType() {
        styledImageView.showWithoutPlaceholder(IMAGE_URL_TEMPLATE, Optional.absent(), URN, imageOperations);

        verify(imageOperations).displayInAdapterView(URN, IMAGE_URL_TEMPLATE, ApiImageSize.getFullImageSize(context().getResources()), squareArtwork, ImageOperations.DisplayType.DEFAULT);
        assertThat(circularArtwork).isNotVisible();
        assertThat(squareArtwork).isVisible();
        assertThat(stationsIndicator).isNotVisible();
    }
}
