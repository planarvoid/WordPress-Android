package com.soundcloud.android.ads;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.stream.StreamItem.Video;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.view.AspectRatioTextureView;
import com.soundcloud.android.view.CircularProgressBar;
import com.soundcloud.android.view.IconToggleButton;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class VideoAdItemRenderer extends AdItemRenderer {

    private final Resources resources;
    private final EventBus eventBus;
    private final CurrentDateProvider currentDateProvider;

    @Inject
    public VideoAdItemRenderer(Resources resources, EventBus eventBus, CurrentDateProvider currentDateProvider) {
        this.resources = resources;
        this.eventBus = eventBus;
        this.currentDateProvider = currentDateProvider;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View adView = LayoutInflater.from(parent.getContext())
                                          .inflate(R.layout.stream_video_ad_card, parent, false);
        adView.setTag(new Holder(adView));
        return adView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<StreamItem> items) {
        final VideoAd videoAd = ((Video) items.get(position)).video();
        final Holder holder = getHolder(itemView);

        holder.headerText.setText(getSponsoredHeaderText(resources, resources.getString(R.string.ads_video)));
        holder.videoView.setAspectRatio(getVideoProportion(videoAd));
        holder.videoView.setVisibility(View.INVISIBLE);
        holder.resetMuteState(holder);

        bindFooter(videoAd, holder);
        bindWhyAdsListener(holder.whyAds);

        holder.volumeButton.setOnClickListener(view -> publishVolumeToggle(position, videoAd, holder));
        holder.playButton.setOnClickListener(view -> publishPlayToggle(position, videoAd));
        holder.videoView.setOnClickListener(view -> handleVideoViewClick(position, videoAd, holder));

        bindVideoSurface(itemView, videoAd);
    }

    void bindVideoSurface(View itemView, VideoAd videoAd) {
        final Holder holder = getHolder(itemView);
        if (listener.isPresent()) {
            listener.get().onVideoTextureBind(holder.videoView, videoAd);
        }
    }

    private void handleVideoViewClick(int position, VideoAd videoAd, Holder holder) {
        if (holder.previouslyUnmuted) {
            publishPlayToggle(position, videoAd);
        } else {
            publishVolumeToggle(position, videoAd, holder);
        }
    }

    private void publishVolumeToggle(int position, VideoAd videoAd, Holder holder) {
        holder.previouslyUnmuted = true;
        eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.ToggleVolume.create(position, videoAd, currentDateProvider.getCurrentDate()));
    }

    private void publishPlayToggle(int position, VideoAd videoAd) {
        eventBus.publish(EventQueue.INLAY_AD, InlayAdEvent.TogglePlayback.create(position, videoAd, currentDateProvider.getCurrentDate()));
    }

    private void bindFooter(VideoAd videoAd, Holder holder) {
        final String callToActionText = videoAd.getCallToActionButtonText().or(resources.getString(R.string.ads_call_to_action));
        final boolean titleIsPresent = videoAd.getTitle().isPresent();

        holder.footerWithTitle.setVisibility(titleIsPresent ? View.VISIBLE : View.GONE);
        holder.callToActionWithoutTitle.setVisibility(titleIsPresent ? View.GONE : View.VISIBLE);

        if (titleIsPresent) {
            holder.title.setText(videoAd.getTitle().get());
            bindCallToAction(videoAd, holder.callToActionWithTitle, callToActionText);
        } else {
            bindCallToAction(videoAd, holder.callToActionWithoutTitle, callToActionText);
        }
    }

    private void bindCallToAction(VideoAd videoAd, TextView callToAction, String callToActionText) {
        callToAction.setText(callToActionText);
        callToAction.setOnClickListener(getClickthroughListener(videoAd));
    }

    public void setPlayState(View itemView, PlaybackStateTransition stateTransition, boolean isMuted) {
        final Holder holder = getHolder(itemView);
        final boolean playbackCompleted = stateTransition.playbackEnded();
        final boolean isVideoViewVisible = holder.videoView.getVisibility() == View.VISIBLE;

        holder.volumeButton.setChecked(!isMuted);
        holder.volumeButton.setVisibility(playbackCompleted ? View.GONE : View.VISIBLE);
        holder.fullscreenButton.setVisibility(playbackCompleted ? View.GONE : View.VISIBLE);

        holder.playButton.setVisibility(stateTransition.isPaused() || playbackCompleted ? View.VISIBLE : View.GONE);
        holder.loadingIndicator.setVisibility(stateTransition.isBuffering() ? View.VISIBLE : View.GONE);

        if (!isVideoViewVisible && stateTransition.isPlayerPlaying()) {
            holder.videoView.setVisibility(View.VISIBLE);
        } else if (playbackCompleted) {
            holder.videoView.setVisibility(View.INVISIBLE);
        }
    }

    public void onViewAttachedToWindow(View itemView, Optional<AdData> adData) {
        if (listener.isPresent() && adData.isPresent() && adData.get() instanceof VideoAd) {
            final Holder holder = getHolder(itemView);
            holder.resetMuteState(holder);
            listener.get().onVideoTextureBind(holder.videoView, (VideoAd) adData.get());
        }
    }

    TextureView getVideoView(View itemView) {
        return getHolder(itemView).videoView;
    }

    private float getVideoProportion(VideoAd videoAd) {
        final VideoAdSource source = videoAd.getFirstSource();
        return (float) source.getHeight() / (float) source.getWidth();
    }

    private Holder getHolder(View adView) {
        return (Holder) adView.getTag();
    }

    static class Holder {
        @BindView(R.id.ad_item) TextView headerText;

        @BindView(R.id.why_ads) TextView whyAds;

        @BindView(R.id.video_view) AspectRatioTextureView videoView;
        @BindView(R.id.video_volume_control) IconToggleButton volumeButton;
        @BindView(R.id.video_fullscreen_control) Button fullscreenButton;
        @BindView(R.id.video_progress) CircularProgressBar loadingIndicator;

        @BindView(R.id.player_play) ImageButton playButton;

        @BindView(R.id.footer_with_title) View footerWithTitle;
        @BindView(R.id.title) TextView title;
        @BindView(R.id.call_to_action_with_title) TextView callToActionWithTitle;

        @BindView(R.id.call_to_action_without_title) TextView callToActionWithoutTitle;

        private boolean previouslyUnmuted;

        Holder(View view) {
            ButterKnife.bind(this, view);
        }

        private void resetMuteState(Holder holder) {
            holder.previouslyUnmuted = false;
        }
    }
}
