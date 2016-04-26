package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.NotNull;

import rx.Subscription;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class AudioAdPresenter extends AdPagePresenter<AudioPlayerAd> implements View.OnClickListener {

    private final ImageOperations imageOperations;
    private final Resources resources;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final AdPageListener listener;
    private final SlideAnimationHelper helper = new SlideAnimationHelper();

    @Inject
    public AudioAdPresenter(ImageOperations imageOperations, Resources resources,
                            PlayerOverlayController.Factory playerOverlayControllerFactory, AdPageListener listener) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.playerOverlayControllerFactory = playerOverlayControllerFactory;
        this.listener = listener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.footer_toggle:
            case R.id.player_play:
            case R.id.artwork_overlay:
            case R.id.fullbleed_ad_artwork:
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
            case R.id.centered_ad_clickable_overlay:
            case R.id.centered_ad_artwork:
            case R.id.cta_button:
                listener.onClickThrough();
                break;
            case R.id.why_ads:
                listener.onAboutAds(view.getContext());
                break;
            case R.id.skip_ad:
                listener.onSkipAd();
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID");
        }
    }

    @Override
    public View createItemView(ViewGroup container, SkipListener skipListener) {
        final View adView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_ad_page, container, false);
        final Holder holder = new Holder(adView, playerOverlayControllerFactory);
        adView.setTag(holder);
        return adView;
    }

    @Override
    public View clearItemView(View convertView) {
        final Holder holder = getViewHolder(convertView);
        holder.previewTitle.setText(Strings.EMPTY);
        resetAdImageLayouts(holder);
        return convertView;
    }

    @Override
    public void bindItemView(View view, AudioPlayerAd playerAd) {
        final Holder holder = getViewHolder(view);
        displayAdvertisement(playerAd, holder);
        displayPreview(playerAd, holder, imageOperations, resources);
        styleCallToActionButton(holder, playerAd, resources);
        setClickListener(this, holder.onClickViews);
        setupSkipButton(holder, playerAd);
    }

    @Override
    public void setProgress(View adView, PlaybackProgress progress) {
        updateSkipStatus(getViewHolder(adView), progress, resources);
    }

    @Override
    public void setPlayState(View adView, PlaybackStateTransition stateTransition, boolean isCurrentTrack, boolean isForeground) {
        final Holder holder = getViewHolder(adView);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();
        holder.playControlsHolder.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);
        holder.footerPlayToggle.setChecked(playSessionIsActive);
        holder.playerOverlayController.setPlayState(stateTransition);
    }

    @Override
    public void setCollapsed(View trackView) {
        onPlayerSlide(trackView, 0);
    }

    @Override
    public void setExpanded(View trackPage, PlayQueueItem playQueueItem, boolean isSelected) {
        onPlayerSlide(trackPage, 1);
    }

    @Override
    public void onPlayerSlide(View trackView, float slideOffset) {
        final Holder holder = getViewHolder(trackView);
        helper.configureViewsFromSlide(slideOffset, holder.footer, holder.close, holder.playerOverlayController);
        holder.close.setVisibility(slideOffset > 0 ? View.VISIBLE : View.GONE);
        holder.whyAds.setEnabled(slideOffset > 0);
    }

    private void resetAdImageLayouts(Holder holder) {
        holder.centeredAdArtworkView.setImageDrawable(null);
        holder.fullbleedAdArtworkView.setImageDrawable(null);
        holder.adImageSubscription.unsubscribe();
        setVisibility(false, holder.companionViews);
    }

    private void displayAdvertisement(AudioPlayerAd playerAd, Holder holder) {
        holder.footerAdvertisement.setText(resources.getString(R.string.ads_advertisement));
        holder.adImageSubscription = imageOperations.adImage(playerAd.getArtwork()).subscribe(getAdImageSubscriber(holder, playerAd));
    }

    @NotNull
    private DefaultSubscriber<Bitmap> getAdImageSubscriber(final Holder holder, final AudioPlayerAd playerAd) {
        return new DefaultSubscriber<Bitmap>(){
            @Override
            public void onNext(Bitmap adImage) {
                if (adImage != null) {
                    updateAdvertisementLayout(holder, adImage, playerAd);
                }
            }
        };
    }

    private void updateAdvertisementLayout(Holder holder, Bitmap adImage, AudioPlayerAd playerAd)  {
        final Optional<String> clickthrough = playerAd.getClickThroughUrl();
        if (isBelowStandardIabSize(adImage.getWidth(), adImage.getHeight())) {
            setCompanionViews(holder.centeredAdArtworkView, holder.centeredAdClickableOverlay, clickthrough.isPresent(), adImage);
        } else {
            setCompanionViews(holder.fullbleedAdArtworkView, holder.ctaButton, clickthrough.isPresent(), adImage);
        }
    }

    private void setCompanionViews(ImageView artworkView, View clickableView, boolean hasCTA, Bitmap adImage) {
        artworkView.setImageBitmap(adImage);
        artworkView.setVisibility(View.VISIBLE);
        clickableView.setVisibility(hasCTA ? View.VISIBLE : View.GONE);
    }

    private boolean isBelowStandardIabSize(int width, int height) {
        return width <= AdConstants.IAB_UNIVERSAL_MED_WIDTH * AdConstants.IAB_UNIVERSAL_MED_MAX_SCALE &&
                height <= AdConstants.IAB_UNIVERSAL_MED_HEIGHT * AdConstants.IAB_UNIVERSAL_MED_MAX_SCALE;
    }

    private Holder getViewHolder(View trackView) {
        return (Holder) trackView.getTag();
    }

    static class Holder extends AdHolder {
        private final ImageView fullbleedAdArtworkView;
        private final ImageView centeredAdArtworkView;
        private final View centeredAdClickableOverlay;
        private final View artworkIdleOverlay;

        private final ToggleButton footerPlayToggle;
        private final View close;

        private final View footer;
        private final TextView footerAdvertisement;

        private final PlayerOverlayController playerOverlayController;

        // View sets
        Iterable<View> onClickViews;
        Iterable<View> companionViews;

        private Subscription adImageSubscription = RxUtils.invalidSubscription();

        Holder(View adView, PlayerOverlayController.Factory playerOverlayControllerFactory) {
            super(adView);
            fullbleedAdArtworkView = (ImageView) adView.findViewById(R.id.fullbleed_ad_artwork);
            centeredAdArtworkView = (ImageView) adView.findViewById(R.id.centered_ad_artwork);
            centeredAdClickableOverlay = adView.findViewById(R.id.centered_ad_clickable_overlay);
            artworkIdleOverlay = adView.findViewById(R.id.artwork_overlay);

            footerPlayToggle = (ToggleButton) adView.findViewById(R.id.footer_toggle);
            close = adView.findViewById(R.id.player_close);

            footer = adView.findViewById(R.id.footer_controls);
            footerAdvertisement = (TextView) adView.findViewById(R.id.footer_ad_text);

            playerOverlayController = playerOverlayControllerFactory.create(artworkIdleOverlay);

            populateViewSets();
        }

        private void populateViewSets() {
            List<View> clickViews = Arrays.asList(centeredAdArtworkView, fullbleedAdArtworkView, centeredAdClickableOverlay,
                    artworkIdleOverlay, playButton, nextButton, previousButton, ctaButton, whyAds, skipAd, previewContainer,
                    footerPlayToggle, close, footer);
            onClickViews = Iterables.filter(clickViews, presentInConfig);
            companionViews = Arrays.asList(centeredAdArtworkView, centeredAdClickableOverlay, fullbleedAdArtworkView, ctaButton);
        }
    }
}
