package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;
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
            case R.id.centered_ad_overlay:
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
        resetSkipButton(holder, resources);
        return adView;
    }

    @Override
    public View clearItemView(View convertView) {
        final Holder holder = getViewHolder(convertView);
        holder.footerAdTitle.setText(Strings.EMPTY);
        holder.previewTitle.setText(Strings.EMPTY);
        resetAdImageLayouts(holder);
        resetSkipButton(holder, resources);
        return convertView;
    }

    @Override
    public void bindItemView(View view, AudioPlayerAd playerAd) {
        final Holder holder = getViewHolder(view);
        displayAdvertisement(playerAd, holder);
        displayPreview(playerAd, holder, imageOperations, resources);
        styleCallToActionButton(holder, playerAd, resources);
        setClickListener(this, holder.onClickViews);
    }

    @Override
    public void setProgress(View adView, PlaybackProgress progress) {
        updateSkipStatus(getViewHolder(adView), progress, resources);
    }

    @Override
    public void setPlayState(View adView, Player.StateTransition stateTransition, boolean isCurrentTrack, boolean isForeground) {
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

    @Override
    public void onViewSelected(View view, PlayQueueItem value, boolean isExpanded) {
        // no-op
    }

    private void resetAdImageLayouts(Holder holder) {
        holder.centeredAdArtworkView.setImageDrawable(null);
        holder.fullbleedAdArtworkView.setImageDrawable(null);
        holder.adImageSubscription.unsubscribe();

        setVisibility(false, holder.centeredAdViews);
        setVisibility(false, holder.fullbleedAdViews);
    }

    private void displayAdvertisement(AudioPlayerAd playerAd, Holder holder) {
        holder.footerAdvertisement.setText(resources.getString(R.string.ads_advertisement));
        holder.footerAdTitle.setText(playerAd.getAdTitle());
        holder.adImageSubscription = imageOperations.adImage(playerAd.getArtwork()).subscribe(getAdImageSubscriber(holder));
    }

    @NotNull
    private DefaultSubscriber<Bitmap> getAdImageSubscriber(final Holder holder) {
        return new DefaultSubscriber<Bitmap>(){
            @Override
            public void onNext(Bitmap adImage) {
                if (adImage != null) {
                    updateAdvertisementLayout(holder, adImage);
                }
            }
        };
    }

    private void updateAdvertisementLayout(Holder holder, Bitmap adImage)  {
        if (isBelowStandardIabSize(adImage.getWidth(), adImage.getHeight())) {
            holder.centeredAdArtworkView.setImageBitmap(adImage);
            setVisibility(true, holder.centeredAdViews);
        } else {
            holder.fullbleedAdArtworkView.setImageBitmap(adImage);
            setVisibility(true, holder.fullbleedAdViews);
        }
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
        private final View centeredAdOverlay;
        private final View artworkIdleOverlay;

        private final ToggleButton footerPlayToggle;
        private final View close;

        private final View footer;
        private final TextView footerAdTitle;
        private final TextView footerAdvertisement;

        private final PlayerOverlayController playerOverlayController;

        // View sets
        Iterable<View> onClickViews;
        Iterable<View> centeredAdViews;
        Iterable<View> fullbleedAdViews;

        private Subscription adImageSubscription = RxUtils.invalidSubscription();

        Holder(View adView, PlayerOverlayController.Factory playerOverlayControllerFactory) {
            super(adView);
            fullbleedAdArtworkView = (ImageView) adView.findViewById(R.id.fullbleed_ad_artwork);
            centeredAdArtworkView = (ImageView) adView.findViewById(R.id.centered_ad_artwork);
            centeredAdOverlay = adView.findViewById(R.id.centered_ad_overlay);
            artworkIdleOverlay = adView.findViewById(R.id.artwork_overlay);

            footerPlayToggle = (ToggleButton) adView.findViewById(R.id.footer_toggle);
            close = adView.findViewById(R.id.player_close);

            footer = adView.findViewById(R.id.footer_controls);
            footerAdTitle = (TextView) adView.findViewById(R.id.footer_title);
            footerAdvertisement = (TextView) adView.findViewById(R.id.footer_user);

            playerOverlayController = playerOverlayControllerFactory.create(artworkIdleOverlay);

            populateViewSets();
        }

        private void populateViewSets() {
            List<View> centeredLayoutViews = Arrays.asList(centeredAdOverlay, centeredAdArtworkView);
            List<View> fullbleedLayoutViews = Arrays.asList(fullbleedAdArtworkView, ctaButton);
            List<View> clickViews = Arrays.asList(centeredAdArtworkView, fullbleedAdArtworkView, centeredAdOverlay,
                    artworkIdleOverlay, playButton, nextButton, previousButton, ctaButton, whyAds, skipAd, previewContainer,
                    footerPlayToggle, close, footer);


            onClickViews = Iterables.filter(clickViews, presentInConfig);
            centeredAdViews = Iterables.filter(centeredLayoutViews, presentInConfig);
            fullbleedAdViews = Iterables.filter(fullbleedLayoutViews, presentInConfig);
        }
    }
}
