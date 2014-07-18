package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.propeller.PropertySet;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ToggleButton;

import javax.inject.Inject;

public class AdPagePresenter implements PagePresenter, View.OnClickListener {
    private final ImageOperations imageOperations;
    private final Resources resources;
    private final PlayerOverlayController artworkController;
    private final TrackPageListener listener;

    @Inject
    public AdPagePresenter(ImageOperations imageOperations, Resources resources, PlayerOverlayController playerOverlayController, TrackPageListener listener) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.artworkController = playerOverlayController;
        this.listener = listener;
    }


    @Override
    public View createItemView(ViewGroup container) {
        final View adView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_ad_page, container, false);
        setupHolder(adView);
        return adView;
    }

    @Override
    public View clearItemView(View convertView) {
        return convertView;
    }

    @Override
    public void bindItemView(View view, PropertySet propertySet) {
        bindItemView(view, new PlayerAd(propertySet));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.footer_toggle:
            case R.id.player_play:
            case R.id.artwork_image_view:
            case R.id.artwork_overlay:
                listener.onTogglePlay();
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID");
        }
    }

    private void bindItemView(View view, PlayerAd playerAd) {
        final Holder holder = getViewHolder(view);
        imageOperations.displayInVisualPlayer(playerAd.getArtwork(), holder.artworkView,
                resources.getDrawable(R.drawable.placeholder));
        setClickListener(holder.getOnClickViews(), this);
    }

    @Override
    public void setProgress(View trackView, PlaybackProgress progress) {
        // no-op
    }

    @Override
    public void setPlayState(View adView, Playa.StateTransition stateTransition, boolean isCurrentTrack) {
        final Holder holder = getViewHolder(adView);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();

        holder.playButton.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);

        holder.footerPlayToggle.setChecked(playSessionIsActive && isCurrentTrack);
        setArtworkPlayState(holder, stateTransition);
    }

    private void setArtworkPlayState(Holder holder, Playa.StateTransition stateTransition) {
        if (stateTransition.playSessionIsActive()) {
            artworkController.hideOverlay(holder.artworkIdleOverlay);
        } else {
            artworkController.darken(holder.artworkIdleOverlay);
        }
    }

    @Override
    public void clearScrubState(View key) {
        // No-op
    }

    public void setExpanded(View trackView, boolean isPlaying) {
        Holder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.GONE);
        setVisibility(holder.getFullScreenViews(), true);
        artworkController.hideOverlay(holder.artworkIdleOverlay);
    }

    public void setCollapsed(View trackView) {
        Holder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.VISIBLE);
        setVisibility(holder.getFullScreenViews(), false);
        artworkController.darken(holder.artworkIdleOverlay);
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

    private void setupHolder(View trackView) {
        Holder holder = new Holder();
        holder.artworkView = (ImageView) trackView.findViewById(R.id.artwork_image_view);
        holder.artworkIdleOverlay = trackView.findViewById(R.id.artwork_overlay);
        holder.playButton = trackView.findViewById(R.id.player_play);
        holder.footerPlayToggle = (ToggleButton) trackView.findViewById(R.id.footer_toggle);
        holder.close = trackView.findViewById(R.id.player_close);

        holder.footer = trackView.findViewById(R.id.footer_controls);

        trackView.setTag(holder);
    }

    static class Holder {
        // Expanded player
        private ImageView artworkView;
        private View artworkIdleOverlay;
        private View playButton;
        private ToggleButton footerPlayToggle;
        private View close;
        // Footer player
        private View footer;

        public View[] getOnClickViews() {
            return new View[] { artworkView, artworkIdleOverlay, playButton, footerPlayToggle };
        }

        public View[] getFullScreenViews() {
            return new View[] { close };
        }
    }

}
