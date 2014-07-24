package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class AdPagePresenter implements PagePresenter, View.OnClickListener {
    private final static int SKIP_DURATION_SEC = 15;

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final AdPageListener listener;

    @Inject
    public AdPagePresenter(ImageOperations imageOperations, Resources resources,
                           PlayerOverlayController.Factory playerOverlayControllerFactory, AdPageListener listener) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.playerOverlayControllerFactory = playerOverlayControllerFactory;
        this.listener = listener;
    }

    @Override
    public View createItemView(ViewGroup container) {
        final View adView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_ad_page, container, false);
        adView.setTag(new Holder(adView, playerOverlayControllerFactory));
        return adView;
    }

    @Override
    public View clearItemView(View convertView) {
        final Holder holder = getViewHolder(convertView);
        holder.footerAdvertiser.setText(ScTextUtils.EMPTY_STRING);
        holder.previewTitle.setText(ScTextUtils.EMPTY_STRING);
        holder.artworkView.setImageDrawable(null);
        return convertView;
    }

    @Override
    public void bindItemView(View view, PropertySet propertySet) {
        bindItemView(view, new PlayerAd(propertySet, resources));
    }

    private void bindItemView(View view, PlayerAd playerAd) {
        final Holder holder = getViewHolder(view);
        holder.footerAdvertisement.setText(resources.getString(R.string.advertisement));
        holder.footerAdvertiser.setText(playerAd.getAdvertiser());
        holder.previewTitle.setText(playerAd.getPreviewTitle());

        imageOperations.displayInVisualPlayer(playerAd.getArtwork(), holder.artworkView,
                resources.getDrawable(R.drawable.placeholder));
        setClickListener(holder.getOnClickViews(), this);
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
            case R.id.player_close:
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
            default:
                throw new IllegalArgumentException("Unexpected view ID");
        }
    }

    @Override
    public void setProgress(View trackPage, PlaybackProgress progress) {
        int secondsUntilSkip = SKIP_DURATION_SEC - ((int) TimeUnit.MILLISECONDS.toSeconds(progress.getPosition()));
        String formattedTime = ScTextUtils.formatSecondsOrMinutes(resources, secondsUntilSkip, TimeUnit.SECONDS);
        getViewHolder(trackPage).timeUntilSkip.setText(formattedTime);
    }

    @Override
    public void setPlayState(View adView, Playa.StateTransition stateTransition, boolean isCurrentTrack) {
        final Holder holder = getViewHolder(adView);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();

        holder.playButton.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);
        holder.footerPlayToggle.setChecked(playSessionIsActive && isCurrentTrack);
        holder.playerOverlayController.update();
    }

    @Override
    public void updateAssociations(View trackPage, PropertySet changeSet) {
        // No-op
    }

    @Override
    public void clearScrubState(View key) {
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
        private final View playButton;
        private final ToggleButton footerPlayToggle;
        private final View close;
        private final TextView previewTitle;
        private final TextView timeUntilSkip;
        private final View learnMore;
        private final View whyAds;
        // Footer player
        private final View footer;
        private final TextView footerAdvertiser;
        private final TextView footerAdvertisement;

        private final PlayerOverlayController playerOverlayController;

        Holder(View adView, PlayerOverlayController.Factory playerOverlayControllerFactory) {
            artworkView = (ImageView) adView.findViewById(R.id.track_page_artwork);
            artworkIdleOverlay = adView.findViewById(R.id.artwork_overlay);
            playButton = adView.findViewById(R.id.player_play);
            footerPlayToggle = (ToggleButton) adView.findViewById(R.id.footer_toggle);
            close = adView.findViewById(R.id.player_close);
            previewTitle = (TextView) adView.findViewById(R.id.preview_title);
            timeUntilSkip = (TextView) adView.findViewById(R.id.time_until_skip);
            learnMore = adView.findViewById(R.id.learn_more);
            whyAds = adView.findViewById(R.id.why_ads);

            footer = adView.findViewById(R.id.footer_controls);
            footerAdvertiser = (TextView) adView.findViewById(R.id.footer_title);
            footerAdvertisement = (TextView) adView.findViewById(R.id.footer_user);

            playerOverlayController = playerOverlayControllerFactory.create(artworkIdleOverlay);
        }

        public View[] getOnClickViews() {
            return new View[] { artworkView, artworkIdleOverlay, playButton,
                    learnMore, whyAds,
                    footerPlayToggle, close, footer };
        }

        public View[] getFullScreenViews() {
            return new View[] { close };
        }
    }

}
