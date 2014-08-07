package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class AdPagePresenter implements PagePresenter, View.OnClickListener {
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final AdPageListener listener;
    private final Context context;

    @Inject
    public AdPagePresenter(ImageOperations imageOperations, Resources resources,
                           PlayerOverlayController.Factory playerOverlayControllerFactory, AdPageListener listener, Context context) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.playerOverlayControllerFactory = playerOverlayControllerFactory;
        this.listener = listener;
        this.context = context;
    }

    @Override
    public View createItemView(ViewGroup container) {
        final View adView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_ad_page, container, false);
        final Holder holder = new Holder(adView, playerOverlayControllerFactory);
        adView.setTag(holder);
        updateCountDown(holder, AdConstants.UNSKIPPABLE_TIME_SECS);
        return adView;
    }

    @Override
    public View clearItemView(View convertView) {
        final Holder holder = getViewHolder(convertView);
        holder.footerAdTitle.setText(ScTextUtils.EMPTY_STRING);
        holder.previewTitle.setText(ScTextUtils.EMPTY_STRING);
        holder.artworkView.setImageDrawable(null);
        toggleSkip(holder, false);
        return convertView;
    }

    @Override
    public void bindItemView(View view, PropertySet propertySet) {
        bindItemView(view, new PlayerAd(propertySet, resources));
    }

    private void bindItemView(View view, PlayerAd playerAd) {
        final Holder holder = getViewHolder(view);
        displayAdvertisement(playerAd, holder);
        displayPreview(playerAd, holder);
        setClickListener(holder.getOnClickViews(), this);
    }

    private void displayAdvertisement(PlayerAd playerAd, Holder holder) {
        holder.footerAdvertisement.setText(resources.getString(R.string.advertisement));
        holder.footerAdTitle.setText(playerAd.getAdTitle());
        imageOperations.displayInVisualPlayer(playerAd.getArtwork(), holder.artworkView, resources.getDrawable(R.drawable.placeholder));
    }

    private void displayPreview(PlayerAd playerAd, Holder holder) {
        holder.previewTitle.setText(playerAd.getPreviewTitle());
        imageOperations.displayWithPlaceholder(playerAd.getMonetizableTrack(), getOptimizedImageSize(), holder.previewArtwork);
    }

    private ApiImageSize getOptimizedImageSize() {
        return ApiImageSize.getListItemImageSize(context);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.footer_toggle:
            case R.id.player_play:
            case R.id.track_page_artwork:
            case R.id.artwork_overlay:
                listener.onTogglePlay();
                break;
            case R.id.track_page_next:
                listener.onNext();
                break;
            case R.id.track_page_previous:
                listener.onPrevious();
                break;
            case R.id.player_close:
            case R.id.preview_container:
                listener.onPlayerClose();
                break;
            case R.id.footer_controls:
                listener.onFooterTap();
                break;
            case R.id.learn_more:
                listener.onClickThrough();
                break;
            case R.id.why_ads:
                listener.onAboutAds();
                break;
            case R.id.skip_ad:
                listener.skipAd();
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID");
        }
    }

    @Override
    public void setProgress(View adView, PlaybackProgress progress) {
        final int secondsUntilSkip = AdConstants.UNSKIPPABLE_TIME_SECS - ((int) TimeUnit.MILLISECONDS.toSeconds(progress.getPosition()));
        final boolean canSkip = secondsUntilSkip <= 0;

        final Holder viewHolder = getViewHolder(adView);
        toggleSkip(viewHolder, canSkip);
        if (!canSkip) {
            updateCountDown(viewHolder, secondsUntilSkip);
        }
    }

    private void toggleSkip(Holder viewHolder, boolean canSkip) {
        viewHolder.skipAd.setVisibility(canSkip ? View.VISIBLE : View.GONE);
        viewHolder.timeUntilSkip.setVisibility(canSkip ? View.GONE : View.VISIBLE);
        viewHolder.previousArea.setEnabled(canSkip);
        viewHolder.nextArea.setEnabled(canSkip);
        viewHolder.previewArtworkOverlay.setVisibility(canSkip ? View.GONE : View.VISIBLE);
    }

    private void updateCountDown(Holder viewHolder, int secondsUntilSkip) {
        String formattedTime = ScTextUtils.formatSecondsOrMinutes(resources, secondsUntilSkip, TimeUnit.SECONDS);
        viewHolder.timeUntilSkip.setText(formattedTime);
    }

    @Override
    public void setPlayState(View adView, Playa.StateTransition stateTransition, boolean isCurrentTrack) {
        final Holder holder = getViewHolder(adView);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();

        holder.playButton.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);
        holder.nextButton.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);
        holder.previousButton.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);
        holder.footerPlayToggle.setChecked(playSessionIsActive && isCurrentTrack);
        holder.playerOverlayController.update();
    }

    @Override
    public void updateAssociations(View trackPage, PropertySet changeSet) {
        // No-op
    }

    @Override
    public void onPageChange(View key) {
        // No-op
    }

    public void setExpanded(View trackView, boolean isPlaying) {
        Holder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.GONE);
        setVisibility(holder.getFullScreenViews(), true);
        holder.playerOverlayController.setExpandedAndUpdate();
    }

    public void setCollapsed(View trackView) {
        Holder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.VISIBLE);
        setVisibility(holder.getFullScreenViews(), false);
        holder.playerOverlayController.setCollapsedAndUpdate();
    }

    private void setClickListener(View[] views, View.OnClickListener listener) {
        for (View v : views) {
            v.setOnClickListener(listener);
        }
    }

    private void setVisibility(View[] views, boolean visible) {
        for (View v : views) {
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private Holder getViewHolder(View trackView) {
        return (Holder) trackView.getTag();
    }

    static class Holder {
        // Expanded player
        private final ImageView artworkView;
        private final View artworkIdleOverlay;
        private final View previewArtworkOverlay;
        private final View playButton;
        private final View nextButton;
        private final View previousButton;
        private final View nextArea;
        private final View previousArea;
        private final ToggleButton footerPlayToggle;
        private final View close;
        private final TextView previewTitle;
        private final TextView timeUntilSkip;
        private final View skipAd;
        private final View previewContainer;
        private final View learnMore;
        private final View whyAds;
        // Footer player
        private final View footer;
        private final TextView footerAdTitle;
        private final TextView footerAdvertisement;

        private final PlayerOverlayController playerOverlayController;
        private final ImageView previewArtwork;

        Holder(View adView, PlayerOverlayController.Factory playerOverlayControllerFactory) {
            artworkView = (ImageView) adView.findViewById(R.id.track_page_artwork);
            artworkIdleOverlay = adView.findViewById(R.id.artwork_overlay);
            previewArtworkOverlay = adView.findViewById(R.id.preview_artwork_overlay);
            playButton = adView.findViewById(R.id.player_play);
            nextButton = adView.findViewById(R.id.player_next);
            previousButton = adView.findViewById(R.id.player_previous);
            previousArea = adView.findViewById(R.id.track_page_previous);
            nextArea = adView.findViewById(R.id.track_page_next);
            footerPlayToggle = (ToggleButton) adView.findViewById(R.id.footer_toggle);
            close = adView.findViewById(R.id.player_close);
            previewTitle = (TextView) adView.findViewById(R.id.preview_title);
            previewArtwork = ((ImageView) adView.findViewById(R.id.preview_artwork));
            timeUntilSkip = (TextView) adView.findViewById(R.id.time_until_skip);
            skipAd = adView.findViewById(R.id.skip_ad);
            previewContainer = adView.findViewById(R.id.preview_container);
            learnMore = adView.findViewById(R.id.learn_more);
            whyAds = adView.findViewById(R.id.why_ads);

            footer = adView.findViewById(R.id.footer_controls);
            footerAdTitle = (TextView) adView.findViewById(R.id.footer_title);
            footerAdvertisement = (TextView) adView.findViewById(R.id.footer_user);

            playerOverlayController = playerOverlayControllerFactory.create(artworkIdleOverlay);
        }

        public View[] getOnClickViews() {
            return new View[] {
                    artworkView, artworkIdleOverlay, playButton,
                    nextArea, previousArea,
                    learnMore, whyAds, skipAd,
                    previewContainer,
                    footerPlayToggle, close, footer
            };
        }

        public View[] getFullScreenViews() {
            return new View[] { close };
        }
    }

}
