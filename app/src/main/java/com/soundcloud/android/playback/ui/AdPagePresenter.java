package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.Playa;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.view.RoundedColorButton;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

class AdPagePresenter implements PlayerPagePresenter, View.OnClickListener {

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final AdPageListener listener;
    private final Context context;
    private final SlideAnimationHelper helper = new SlideAnimationHelper();

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
    public View createItemView(ViewGroup container, SkipListener skipListener) {
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
    public void bindItemView(View view, PropertySet propertySet, boolean isCurrentTrack, boolean isForeground, ViewVisibilityProvider viewVisibilityProvider) {
        bindItemView(view, new PlayerAd(propertySet, resources));
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
            case R.id.player_next:
                listener.onNext();
                break;
            case R.id.player_previous:
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
                listener.onAboutAds((FragmentActivity) view.getContext());
                break;
            case R.id.skip_ad:
                listener.onSkipAd();
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

    @Override
    public void setPlayState(View adView, Playa.StateTransition stateTransition, boolean isCurrentTrack, boolean isForeground) {
        final Holder holder = getViewHolder(adView);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();
        holder.playControlsHolder.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);
        holder.footerPlayToggle.setChecked(playSessionIsActive);
        holder.playerOverlayController.setPlayState(stateTransition);
    }

    @Override
    public void onPlayableUpdated(View trackPage, EntityStateChangedEvent trackChangedEvent) {
        // no-op
    }

    @Override
    public void onBackground(View trackPage) {
        // no-op
    }

    @Override
    public void onForeground(View trackPage) {
        // no-op
    }

    @Override
    public void setCollapsed(View trackView) {
        onPlayerSlide(trackView, 0);
    }

    @Override
    public void setExpanded(View trackView) {
        onPlayerSlide(trackView, 1);
    }

    @Override
    public void onPlayerSlide(View trackView, float slideOffset) {
        final Holder holder = getViewHolder(trackView);
        helper.configureViewsFromSlide(slideOffset, holder.footer, holder.close, holder.playerOverlayController);
    }

    @Override
    public void clearAdOverlay(View trackPage) {
        // no-op
    }

    @Override
    public void setCastDeviceName(View trackPage, String deviceName) {
        // no-op
    }

    private void bindItemView(View view, PlayerAd playerAd) {
        final Holder holder = getViewHolder(view);
        displayAdvertisement(playerAd, holder);
        displayPreview(playerAd, holder);
        styleLearnMoreButton(holder, playerAd);
        setClickListener(this, holder.onClickViews);
    }

    private void styleLearnMoreButton(Holder holder, PlayerAd playerAd) {
        holder.learnMore.setTextColor(getColorStates(
                playerAd.getFocusedTextColor(),
                playerAd.getPressedTextColor(),
                playerAd.getDefaultTextColor()
        ));
        holder.learnMore.setBackground(getColorStates(
                playerAd.getFocusedBackgroundColor(),
                playerAd.getPressedBackgroundColor(),
                playerAd.getDefaultBackgroundColor()
        ));
    }

    private ColorStateList getColorStates(int focusedColor, int pressedColor, int defaultColor) {
        return new ColorStateList(new int[][]{
                new int[]{android.R.attr.state_focused},
                new int[]{android.R.attr.state_pressed},
                new int[]{},
        }, new int[]{
                focusedColor,
                pressedColor,
                defaultColor});
    }

    private void displayAdvertisement(PlayerAd playerAd, Holder holder) {
        holder.footerAdvertisement.setText(resources.getString(R.string.advertisement));
        holder.footerAdTitle.setText(playerAd.getAdTitle());
        imageOperations.displayAdInPlayer(playerAd.getArtwork(), holder.artworkView, resources.getDrawable(R.drawable.placeholder));
    }

    private void displayPreview(PlayerAd playerAd, Holder holder) {
        holder.previewTitle.setText(playerAd.getPreviewTitle());
        imageOperations.displayWithPlaceholder(playerAd.getMonetizableTrack(), getOptimizedImageSize(), holder.previewArtwork);
    }

    private ApiImageSize getOptimizedImageSize() {
        return ApiImageSize.getListItemImageSize(context);
    }

    private void toggleSkip(Holder holder, boolean canSkip) {
        holder.skipAd.setVisibility(canSkip ? View.VISIBLE : View.GONE);
        holder.timeUntilSkip.setVisibility(canSkip ? View.GONE : View.VISIBLE);
        holder.previewArtworkOverlay.setVisibility(canSkip ? View.GONE : View.VISIBLE);
        setEnabled(canSkip, holder.skipDisableViews);
    }

    private void updateCountDown(Holder viewHolder, int secondsUntilSkip) {
        String formattedTime = ScTextUtils.formatSecondsOrMinutes(resources, secondsUntilSkip, TimeUnit.SECONDS);
        viewHolder.timeUntilSkip.setText(resources.getString(R.string.ad_skip, formattedTime));
    }

    private void setClickListener(View.OnClickListener listener, Iterable<View> views) {
        for (View v : views) {
            v.setOnClickListener(listener);
        }
    }

    private void setEnabled(boolean enabled, Iterable<View> views) {
        for (View v : views) {
            v.setEnabled(enabled);
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
        private final ToggleButton footerPlayToggle;
        private final View close;
        private final TextView previewTitle;
        private final TextView timeUntilSkip;
        private final View skipAd;
        private final View previewContainer;
        private final RoundedColorButton learnMore;
        private final View whyAds;
        private final View playControlsHolder;

        // Footer player
        private final View footer;
        private final TextView footerAdTitle;
        private final TextView footerAdvertisement;
        private final ImageView previewArtwork;
        private final PlayerOverlayController playerOverlayController;
        private final Predicate<View> presentInConfig = new Predicate<View>() {
            @Override
            public boolean apply(@Nullable View v) {
                return v != null;
            }
        };
        // View sets
        Iterable<View> skipDisableViews;
        Iterable<View> onClickViews;

        Holder(View adView, PlayerOverlayController.Factory playerOverlayControllerFactory) {
            artworkView = (ImageView) adView.findViewById(R.id.track_page_artwork);
            artworkIdleOverlay = adView.findViewById(R.id.artwork_overlay);
            previewArtworkOverlay = adView.findViewById(R.id.preview_artwork_overlay);
            playButton = adView.findViewById(R.id.player_play);
            nextButton = adView.findViewById(R.id.player_next);
            previousButton = adView.findViewById(R.id.player_previous);
            footerPlayToggle = (ToggleButton) adView.findViewById(R.id.footer_toggle);
            close = adView.findViewById(R.id.player_close);
            previewTitle = (TextView) adView.findViewById(R.id.preview_title);
            previewArtwork = ((ImageView) adView.findViewById(R.id.preview_artwork));
            timeUntilSkip = (TextView) adView.findViewById(R.id.time_until_skip);
            skipAd = adView.findViewById(R.id.skip_ad);
            previewContainer = adView.findViewById(R.id.preview_container);
            learnMore = (RoundedColorButton) adView.findViewById(R.id.learn_more);
            whyAds = adView.findViewById(R.id.why_ads);
            playControlsHolder = adView.findViewById(R.id.play_controls);

            footer = adView.findViewById(R.id.footer_controls);
            footerAdTitle = (TextView) adView.findViewById(R.id.footer_title);
            footerAdvertisement = (TextView) adView.findViewById(R.id.footer_user);

            playerOverlayController = playerOverlayControllerFactory.create(artworkIdleOverlay);

            populateViewSets();
        }

        private void populateViewSets() {
            List<View> disableViews = Arrays.asList(previousButton, nextButton);
            List<View> clickViews = Arrays.asList(artworkView, artworkIdleOverlay, playButton, nextButton,
                    previousButton, learnMore, whyAds, skipAd, previewContainer, footerPlayToggle, close, footer);

            skipDisableViews = Iterables.filter(disableViews, presentInConfig);
            onClickViews = Iterables.filter(clickViews, presentInConfig);
        }

    }

}
