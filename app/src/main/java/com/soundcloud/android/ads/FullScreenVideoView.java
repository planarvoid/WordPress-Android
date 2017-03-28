package com.soundcloud.android.ads;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.playback.ui.view.RoundedColorButton;
import com.soundcloud.android.view.AspectRatioTextureView;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import javax.inject.Inject;

class FullScreenVideoView {

    private final VideoSurfaceProvider videoSurfaceProvider;
    private final Resources resources;

    interface Listener {
        void onTogglePlayClick();
        void onShrinkClick();
        void onLearnMoreClick(Context context);
    }

    private Optional<Listener> listener = Optional.absent();

    @BindView(R.id.video_view) AspectRatioTextureView videoView;
    @BindView(R.id.player_play) View playButton;
    @BindView(R.id.video_progress) View loadingIndicator;
    @BindView(R.id.video_shrink_control) View shrinkControl;
    @BindView(R.id.cta_button) RoundedColorButton ctaButton;

    @Inject
    FullScreenVideoView(VideoSurfaceProvider videoSurfaceProvider, Resources resources) {
        this.videoSurfaceProvider = videoSurfaceProvider;
        this.resources = resources;
    }

    void setListener(Listener listener) {
        this.listener = Optional.of(listener);
    }

    void setupContentView(AppCompatActivity activity, VideoAd videoAd) {
        ButterKnife.bind(this, activity.findViewById(android.R.id.content));
        videoView.setAspectRatio(videoAd.getVideoProportion());
        setupCallToActionButton(videoAd);
        setupClickListeners();
    }

    private void setupClickListeners() {
        listener.ifPresent(callback -> {
            final View.OnClickListener toggleListener = view -> callback.onTogglePlayClick();
            videoView.setOnClickListener(toggleListener);
            playButton.setOnClickListener(toggleListener);
            shrinkControl.setOnClickListener(view -> callback.onShrinkClick());
            ctaButton.setOnClickListener(view -> callback.onLearnMoreClick(view.getContext()));
        });
    }

    private ColorStateList getColorStates(String focusedColor, String pressedColor, String defaultColor) {
        return new ColorStateList(new int[][]{
                new int[]{android.R.attr.state_focused},
                new int[]{android.R.attr.state_pressed},
                new int[]{},
        }, new int[]{Color.parseColor(focusedColor), Color.parseColor(pressedColor), Color.parseColor(defaultColor)});
    }

    private void setupCallToActionButton(VideoAd videoAd) {
        final String defaultText = resources.getString(R.string.ads_call_to_action);
        ctaButton.setText(videoAd.getCallToActionButtonText().or(defaultText));
        videoAd.getDisplayProperties().ifPresent(properties ->  {
            ctaButton.setTextColor(getColorStates(
                    properties.getFocusedTextColor(),
                    properties.getPressedTextColor(),
                    properties.getDefaultTextColor()
            ));
            ctaButton.setBackground(getColorStates(
                    properties.getFocusedBackgroundColor(),
                    properties.getPressedBackgroundColor(),
                    properties.getDefaultBackgroundColor()
            ));
        });
    }

    void setPlayState(PlaybackStateTransition transition) {
        playButton.setVisibility(transition.isPaused() || transition.playbackEnded() ? View.VISIBLE : View.GONE);
        loadingIndicator.setVisibility(transition.isBuffering() ? View.VISIBLE : View.GONE);
    }

    void bindVideoSurface(String uuid, VideoSurfaceProvider.Origin origin) {
        videoSurfaceProvider.setTextureView(uuid, origin, videoView);
    }

    void unbindVideoSurface(VideoSurfaceProvider.Origin origin) {
        videoSurfaceProvider.onDestroy(origin);
    }
}
