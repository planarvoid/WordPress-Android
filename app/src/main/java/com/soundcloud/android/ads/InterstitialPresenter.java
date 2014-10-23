package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class InterstitialPresenter extends AdOverlayPresenter {

    private final View previewContainer;
    private final View interstitialImageHolder;
    private final ImageView previewImage;
    private final TextView nowPlayingTitleView;
    private final Resources resources;

    public InterstitialPresenter(View trackView, Listener listener, EventBus eventBus, ImageOperations imageOperations, Resources resources) {
        super(trackView, R.id.interstitial, R.id.interstitial_stub, R.id.interstitial_image, R.id.interstitial_header, listener, imageOperations, eventBus);
        this.previewContainer = trackView.findViewById(R.id.interstitial_preview_container);
        this.previewImage = (ImageView) trackView.findViewById(R.id.interstitial_now_playing_artwork);
        this.nowPlayingTitleView = (TextView) trackView.findViewById(R.id.interstitial_now_playing_title);
        this.resources = resources;
        this.interstitialImageHolder = trackView.findViewById(R.id.interstitial_image_holder);
    }

    @Override
    public boolean shouldDisplayOverlay(PropertySet data, boolean isExpanded, boolean isPortrait, boolean isForeground) {
        return isExpanded && isForeground && !data.getOrElse(AdOverlayProperty.META_AD_DISMISSED, false)
                && resources.getBoolean(R.bool.allow_interstitials);
    }

    @Override
    public boolean isFullScreen() {
        return true;
    }

    @Override
    public void setVisible() {
        super.setVisible();
        interstitialImageHolder.setVisibility(View.VISIBLE);
        previewContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void setInvisible() {
        super.setInvisible();
        interstitialImageHolder.setVisibility(View.GONE);
        previewContainer.setVisibility(View.GONE);
    }

    @Override
    public void bind(PropertySet data) {
        super.bind(data);
        final ApiImageSize listItemImageSize = ApiImageSize.getListItemImageSize(previewImage.getResources());
        imageOperations.displayWithPlaceholder(data.get(TrackProperty.URN), listItemImageSize, previewImage);

        final String nowPlayingCreator = data.get(TrackProperty.CREATOR_NAME);
        final String nowPlayingTitle = data.get(TrackProperty.TITLE);
        nowPlayingTitleView.setText(resources.getString(R.string.now_playing, nowPlayingTitle, nowPlayingCreator));
    }
}
