package com.soundcloud.android.ads;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.playback.playqueue.PlayQueueUIEvent;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

@AutoFactory(allowSubclasses = true)
public class InterstitialPresenter extends AdOverlayPresenter {

    private final View previewContainer;
    private final View interstitialImageHolder;
    private final ImageView previewImage;
    private final TextView nowPlayingTitleView;
    private final Resources resources;

    public InterstitialPresenter(View trackView, Listener listener,
                                 @Provided EventBus eventBus,
                                 @Provided ImageOperations imageOperations,
                                 @Provided Resources resources) {
        super(trackView,
              R.id.interstitial,
              R.id.interstitial_stub,
              R.id.interstitial_image,
              R.id.interstitial_image_holder,
              R.id.interstitial_header,
              listener,
              imageOperations,
              eventBus);
        this.previewContainer = trackView.findViewById(R.id.interstitial_preview_container);
        this.previewImage = (ImageView) trackView.findViewById(R.id.interstitial_now_playing_artwork);
        this.nowPlayingTitleView = (TextView) trackView.findViewById(R.id.interstitial_now_playing_title);
        this.resources = resources;
        this.interstitialImageHolder = trackView.findViewById(R.id.interstitial_image_holder);
    }

    @Override
    public boolean shouldDisplayOverlay(OverlayAdData data,
                                        boolean isExpanded,
                                        boolean isPortrait,
                                        boolean isForeground) {
        return isExpanded && isForeground && !data.isMetaAdDismissed() && resources.getBoolean(R.bool.allow_interstitials);
    }

    @Override
    public boolean isFullScreen() {
        return true;
    }

    @Override
    public void onAdVisible(PlayQueueItem playQueueItem, OverlayAdData data, TrackSourceInfo trackSourceInfo) {
        super.onAdVisible(playQueueItem, data, trackSourceInfo);
        interstitialImageHolder.setVisibility(View.VISIBLE);
        previewContainer.setVisibility(View.VISIBLE);
        eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createHideEvent());
    }

    @Override
    public void onAdNotVisible() {
        super.onAdNotVisible();
        interstitialImageHolder.setVisibility(View.GONE);
        previewContainer.setVisibility(View.GONE);
    }

    @Override
    public void bind(OverlayAdData data) {
        super.bind(data);
        final ApiImageSize listItemImageSize = ApiImageSize.getListItemImageSize(previewImage.getResources());
        imageOperations.displayWithPlaceholder(data.getMonetizableTrackUrn(), listItemImageSize, previewImage);

        if (data.getMonetizableTitle() != null && data.getMonetizableCreator() != null) {
            final String nowPlayingTitle = data.getMonetizableTitle();
            final String nowPlayingCreator = data.getMonetizableCreator();
            nowPlayingTitleView.setText(resources.getString(R.string.ads_now_playing_tracktitle_username,
                                                            nowPlayingTitle,
                                                            nowPlayingCreator));
        } else {
            nowPlayingTitleView.setText(R.string.ads_now_playing);
            // we are missing certain track data here, just before launching.
            // Need to look at these reports and find the path that causes this. It seems to be when a monetizable track
            // has an audio ad, then an interstitial when the same playlist is started over
            ErrorUtils.handleSilentException(new IllegalStateException("Interstitial missing track data: " + data));
        }
    }
}
