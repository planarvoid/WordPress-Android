package com.soundcloud.android.image;

import static butterknife.ButterKnife.findById;
import static com.soundcloud.android.image.ImageStyle.SQUARE;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * This custom view wraps the logic to show artwork as a circle, square, or with station overlay, according to the passed {@link ImageStyle}
 */
public class StyledImageView extends FrameLayout {
    private final ImageView circularArtwork;
    private final ImageView squareArtwork;
    private final ImageView stationIndicator;

    public StyledImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.styled_image_view, this);
        circularArtwork = findById(this, R.id.circular_artwork);
        squareArtwork = findById(this, R.id.square_artwork);
        stationIndicator = findById(this, R.id.station_indicator);
    }

    public void showWithoutPlaceholder(Optional<String> imageUrlTemplate, Optional<ImageStyle> imageStyle, Optional<Urn> urn, ImageOperations imageOperations) {
        show(urn, imageUrlTemplate, imageStyle, imageOperations, false);
    }

    public void showWithPlaceholder(Optional<String> imageUrlTemplate, Optional<ImageStyle> imageStyle, Optional<Urn> urn, ImageOperations imageOperations) {
        show(urn, imageUrlTemplate, imageStyle, imageOperations, true);
    }

    private void show(Optional<Urn> urn, Optional<String> imageUrlTemplate, Optional<ImageStyle> imageStyle, ImageOperations imageOperations, boolean usePlaceholder) {
        switch (imageStyle.or(SQUARE)) {
            case SQUARE:
                displaySquare(urn, imageUrlTemplate, imageOperations, usePlaceholder);
                squareArtwork.setVisibility(VISIBLE);
                circularArtwork.setVisibility(GONE);
                stationIndicator.setVisibility(GONE);
                break;
            case CIRCULAR:
                displayCircular(urn, imageUrlTemplate, imageOperations, usePlaceholder);
                circularArtwork.setVisibility(VISIBLE);
                squareArtwork.setVisibility(GONE);
                stationIndicator.setVisibility(GONE);
                break;
            case STATION:
                displaySquare(urn, imageUrlTemplate, imageOperations, usePlaceholder);
                squareArtwork.setVisibility(VISIBLE);
                stationIndicator.setVisibility(VISIBLE);
                circularArtwork.setVisibility(GONE);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown ImageType %s", imageStyle.get()));
        }
    }

    private void displayCircular(Optional<Urn> urn, Optional<String> imageUrlTemplate, ImageOperations imageOperations, boolean usePlaceholder) {
        if (usePlaceholder) {
            imageOperations.displayCircularWithPlaceholder(urn, imageUrlTemplate, ApiImageSize.getFullImageSize(getContext().getResources()), circularArtwork);
        } else {
            imageOperations.displayCircularInAdapterView(urn, imageUrlTemplate, ApiImageSize.getFullImageSize(getContext().getResources()), circularArtwork);
        }
    }

    private void displaySquare(Optional<Urn> urn, Optional<String> imageUrlTemplate, ImageOperations imageOperations, boolean usePlaceholder) {
        if (usePlaceholder) {
            imageOperations.displayWithPlaceholder(urn, imageUrlTemplate, ApiImageSize.getFullImageSize(getContext().getResources()), squareArtwork);
        } else {
            imageOperations.displayInAdapterView(urn, imageUrlTemplate, ApiImageSize.getFullImageSize(getContext().getResources()), squareArtwork);
        }
    }
}
