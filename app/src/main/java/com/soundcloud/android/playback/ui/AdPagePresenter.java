package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.utils.ViewUtils.forEach;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.view.RoundedColorButton;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

abstract class AdPagePresenter<T extends PlayerAd> implements PlayerPagePresenter<T> {

    @Override
    abstract public void bindItemView(View view, T playerItem);

    @Override
    public void onPlayableReposted(View trackPage, RepostsStatusEvent.RepostStatus repostStatus) {
        // default no-op
    }

    @Override
    public void onPlayableLiked(View trackPage, LikesStatusEvent.LikeStatus likeStatus) {
        // default no-op
    }

    @Override
    public void onBackground(View trackPage) {
        // default no-op
    }

    @Override
    public void onForeground(View trackPage) {
        // default no-op
    }

    @Override
    public void onDestroyView(View trackPage) {
        // default no-op
    }

    @Override
    public void clearAdOverlay(View trackPage) {
        // default no-op
    }

    @Override
    public void updateCastData(View trackPage, boolean animate) {
        // default no-op
    }

    @Override
    public void setCollapsed(View adPage) {
        // default no-op
    }

    @Override
    public void setExpanded(View trackPage, PlayQueueItem playQueueItem, boolean isSelected) {
        // default no-op
    }

    @Override
    public void onPlayerSlide(View adPage, float position) {
        // default no-op
    }

    @Override
    public void onViewSelected(View view, PlayQueueItem value, boolean isExpanded) {
        // default no-op
    }

    @Override
    public void onItemAdded(View view) {
        // default no-op
    }

    @Override
    public void showIntroductoryOverlayForPlayQueue(View view) {
        // default no-op
    }

    void setClickListener(View.OnClickListener listener, Iterable<View> views) {
        forEach(views, view -> view.setOnClickListener(listener));
    }

    void setEnabled(boolean enabled, Iterable<View> views) {
        forEach(views, v -> v.setEnabled(enabled));
    }

    void setVisibility(boolean visible, Iterable<View> views) {
        forEach(views, v -> v.setVisibility(visible ? View.VISIBLE : View.INVISIBLE));
    }

    void setAnimation(Iterable<View> views, Animation animation) {
        forEach(views, view -> {
            view.startAnimation(animation);
            view.setVisibility(View.INVISIBLE);
        });
    }

    void clearAnimation(Iterable<View> views) {
        forEach(views, View::clearAnimation);
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

    void styleCallToActionButton(AdHolder holder, PlayerAd playerAd, Resources resources) {
        holder.ctaButton.setText(playerAd.getCallToActionButtonText(resources));
        if (playerAd.hasVisualAdProperties()) {
            holder.ctaButton.setTextColor(getColorStates(
                    playerAd.getFocusedTextColor(),
                    playerAd.getPressedTextColor(),
                    playerAd.getDefaultTextColor()
            ));
            holder.ctaButton.setBackground(getColorStates(
                    playerAd.getFocusedBackgroundColor(),
                    playerAd.getPressedBackgroundColor(),
                    playerAd.getDefaultBackgroundColor()
            ));
        }
    }

    void displayPreview(PlayerAd playerAd, AdHolder holder, ImageOperations imageOperations, Resources resources) {
        final ApiImageSize previewSize = ApiImageSize.getListItemImageSize(resources);
        holder.previewTitle.setText(playerAd.getPreviewTitle(resources));
        imageOperations.displayWithPlaceholder(
                playerAd.getMonetizableTrack(),
                Optional.absent(),
                previewSize,
                holder.previewArtwork);
    }

    void setupSkipButton(AdHolder holder, T ad) {
        final boolean skippable = ad.getAdData().isSkippable();
        holder.setSkippable(skippable);
        holder.timeUntilSkip.setText(Strings.EMPTY);
        holder.skipAd.setVisibility(View.GONE);
    }

    void updateSkipStatus(AdHolder holder, PlaybackProgress progress, Resources resources) {
        final int fullDuration = (int) TimeUnit.MILLISECONDS.toSeconds(progress.getDuration());
        final int skipDuration = holder.isSkippable
                                 ? Math.min(AdConstants.UNSKIPPABLE_TIME_SECS, fullDuration)
                                 : fullDuration;
        final int secondsUntilSkip = skipDuration - ((int) TimeUnit.MILLISECONDS.toSeconds(progress.getPosition()));
        final boolean canSkip = secondsUntilSkip <= 0;

        toggleSkip(holder, canSkip);
        if (secondsUntilSkip > 0) {
            updateSkipCountDown(holder, secondsUntilSkip, fullDuration, resources);
        }
    }

    private void toggleSkip(AdHolder holder, boolean canSkip) {
        holder.skipAd.setVisibility(canSkip ? View.VISIBLE : View.GONE);
        holder.timeUntilSkip.setVisibility(canSkip ? View.GONE : View.VISIBLE);
        holder.previewArtworkOverlay.setVisibility(canSkip ? View.GONE : View.VISIBLE);
        setEnabled(canSkip, holder.skipDisableViews);
    }

    private void updateSkipCountDown(AdHolder viewHolder, int secondsUntilSkip, int fullDuration, Resources resources) {
        String formattedTime = ScTextUtils.formatSecondsOrMinutes(resources, secondsUntilSkip, TimeUnit.SECONDS);
        String timerText = viewHolder.isSkippable && fullDuration > AdConstants.UNSKIPPABLE_TIME_SECS
                           ? resources.getString(R.string.ads_skip_in_time, formattedTime)
                           : formattedTime;

        viewHolder.timeUntilSkip.setText(timerText);
    }

    static class AdHolder {
        final View playButton;
        final View nextButton;
        final View previousButton;
        final View playControlsHolder;

        final View skipAd;
        final TextView timeUntilSkip;

        final View previewArtworkOverlay;
        final View previewContainer;
        final TextView previewTitle;
        final ImageView previewArtwork;

        final RoundedColorButton ctaButton;
        final View whyAds;
        final View advertisement;

        final Iterable<View> skipDisableViews;
        boolean isSkippable;

        final Predicate<View> presentInConfig = v -> v != null;

        AdHolder(View adView) {
            this.playButton = adView.findViewById(R.id.player_play);
            this.nextButton = adView.findViewById(R.id.player_next);
            this.previousButton = adView.findViewById(R.id.player_previous);
            this.playControlsHolder = adView.findViewById(R.id.play_controls);
            this.skipAd = adView.findViewById(R.id.skip_ad);
            this.timeUntilSkip = adView.findViewById(R.id.time_until_skip);
            this.previewArtworkOverlay = adView.findViewById(R.id.preview_artwork_overlay);
            this.previewContainer = adView.findViewById(R.id.preview_container);
            this.previewTitle = adView.findViewById(R.id.preview_title);
            this.previewArtwork = adView.findViewById(R.id.preview_artwork);
            this.ctaButton = adView.findViewById(R.id.cta_button);
            this.whyAds = adView.findViewById(R.id.why_ads);
            this.advertisement = adView.findViewById(R.id.advertisement);

            skipDisableViews = Iterables.filter(Arrays.asList(previousButton, nextButton), presentInConfig);
        }

        void setSkippable(boolean skippable) {
            isSkippable = skippable;
        }
    }
}
