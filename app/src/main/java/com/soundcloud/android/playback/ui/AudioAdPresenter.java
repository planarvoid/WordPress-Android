package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.util.AnimUtils;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

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
    private final PlayerArtworkLoader artworkLoader;
    private final SlideAnimationHelper helper = new SlideAnimationHelper();

    @Inject
    public AudioAdPresenter(ImageOperations imageOperations, Resources resources,
                            PlayerOverlayController.Factory playerOverlayControllerFactory, AdPageListener listener,
                            PlayerArtworkLoader artworkLoader) {
        this.imageOperations = imageOperations;
        this.resources = resources;
        this.playerOverlayControllerFactory = playerOverlayControllerFactory;
        this.listener = listener;
        this.artworkLoader = artworkLoader;
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
                listener.onClickThrough(view.getContext());
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
        final View adView = LayoutInflater.from(container.getContext())
                                          .inflate(R.layout.player_ad_page, container, false);
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
        setupAdVisual(playerAd, holder);
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
    public void setPlayState(View adView,
                             PlayStateEvent playStateEvent,
                             boolean isCurrentTrack,
                             boolean isForeground) {
        final Holder holder = getViewHolder(adView);
        final boolean playSessionIsActive = playStateEvent.playSessionIsActive();
        holder.playControlsHolder.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);

        if (playSessionIsActive && holder.isCompanionless) {
            AnimUtils.showView(holder.companionlessText, true);
        } else {
            holder.companionlessText.setVisibility(View.GONE);
        }
        holder.footerPlayToggle.setChecked(playSessionIsActive);
        holder.playerOverlayController.setPlayState(playStateEvent);
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

    @Override
    public void updatePlayQueueButton(View view) {
        // no-op
    }

    private void resetAdImageLayouts(Holder holder) {
        holder.centeredAdArtworkView.setImageDrawable(null);
        holder.fullbleedAdArtworkView.setImageDrawable(null);
        holder.playerOverlayController.setAdOverlayShown(false);
        holder.companionlessText.setVisibility(View.GONE);
        holder.isCompanionless = false;
        holder.adImageSubscription.unsubscribe();
        setVisibility(false, holder.companionViews);
    }

    private void setupAdVisual(AudioPlayerAd playerAd, final Holder holder) {
        holder.footerAdvertisement.setText(resources.getString(R.string.ads_advertisement));
        if (playerAd.hasCompanion()) {
            holder.adImageSubscription = imageOperations.bitmap(playerAd.getImage().get())
                    .subscribe(new AdImageSubscriber(holder, playerAd));
        } else {
            // Companionless audio ads use blurred artwork of monetizable track for background
            holder.adImageSubscription = artworkLoader.loadAdBackgroundImage(playerAd.getMonetizableTrack())
                    .subscribe(new AdImageSubscriber(holder, playerAd));
            holder.playerOverlayController.setAdOverlayShown(true);
            holder.companionlessText.setVisibility(View.VISIBLE);
            holder.isCompanionless = true;
        }
    }

    private final class AdImageSubscriber extends DefaultSubscriber<Bitmap> {
        private Holder holder;
        private AudioPlayerAd audioPlayerAd;

        public AdImageSubscriber(Holder holder, AudioPlayerAd audioPlayerAd) {
            this.holder = holder;
            this.audioPlayerAd = audioPlayerAd;
        }

        @Override
        public void onNext(Bitmap adImage) {
            if (adImage != null) {
                updateAdvertisementLayout(holder, adImage, audioPlayerAd);
            }
        }
    }

    private void updateAdvertisementLayout(Holder holder, Bitmap adImage, AudioPlayerAd playerAd) {
        final Optional<String> clickthrough = playerAd.getClickThroughUrl();
        if (isBelowStandardIabSize(adImage.getWidth(), adImage.getHeight()) && playerAd.hasCompanion()) {
            setCompanionViews(holder.centeredAdArtworkView,
                              holder.centeredAdClickableOverlay,
                              clickthrough.isPresent(),
                              adImage);
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
        private final View companionlessText;

        private final ToggleButton footerPlayToggle;
        private final View close;

        private final View footer;
        private final TextView footerAdvertisement;

        private final PlayerOverlayController playerOverlayController;

        boolean isCompanionless;

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
            companionlessText = adView.findViewById(R.id.companionless_ad_text);

            footerPlayToggle = (ToggleButton) adView.findViewById(R.id.footer_toggle);
            close = adView.findViewById(R.id.player_close);

            footer = adView.findViewById(R.id.footer_controls);
            footerAdvertisement = (TextView) adView.findViewById(R.id.footer_ad_text);

            playerOverlayController = playerOverlayControllerFactory.create(artworkIdleOverlay);

            populateViewSets();
        }

        private void populateViewSets() {
            List<View> clickViews = Arrays.asList(centeredAdArtworkView,
                                                  fullbleedAdArtworkView,
                                                  centeredAdClickableOverlay,
                                                  artworkIdleOverlay,
                                                  playButton,
                                                  nextButton,
                                                  previousButton,
                                                  ctaButton,
                                                  whyAds,
                                                  skipAd,
                                                  previewContainer,
                                                  footerPlayToggle,
                                                  close,
                                                  footer);
            onClickViews = Iterables.filter(clickViews, presentInConfig);
            companionViews = Arrays.asList(centeredAdArtworkView,
                                           centeredAdClickableOverlay,
                                           fullbleedAdArtworkView,
                                           ctaButton);
        }
    }
}
