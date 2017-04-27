package com.soundcloud.android.ads;

import static com.soundcloud.android.utils.ViewUtils.forEach;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.VideoSurfaceProvider;
import com.soundcloud.android.playback.ui.view.RoundedColorButton;
import com.soundcloud.android.view.AspectRatioTextureView;
import com.soundcloud.java.optional.Optional;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

class FullScreenVideoView {

    private static final long FADE_OUT_DURATION = TimeUnit.SECONDS.toMillis(1L);
    private static final long FADE_OUT_OFFSET = TimeUnit.SECONDS.toMillis(2L);
    private static final float ACCELLERATOR_FACTOR = 2.0f;

    private final VideoSurfaceProvider videoSurfaceProvider;
    private final Resources resources;

    interface Listener {
        void onTogglePlayClick();
        void onShrinkClick();
        void onLearnMoreClick(Context context);
    }

    private Optional<Listener> listener = Optional.absent();
    private Iterable<View> fadingViews = Collections.emptyList();

    @BindView(R.id.video_view) AspectRatioTextureView videoView;
    @BindView(R.id.player_play) View playButton;
    @BindView(R.id.video_progress) View loadingIndicator;
    @BindView(R.id.video_shrink_control) View shrinkControl;
    @BindView(R.id.cta_button) RoundedColorButton ctaButton;
    @BindView(R.id.video_gradient) View gradient;
    @BindView(R.id.viewability_layer) View viewabilityLayer;

    @Inject
    FullScreenVideoView(VideoSurfaceProvider surfaceProvider,
                        Resources resources) {
        this.videoSurfaceProvider = surfaceProvider;
        this.resources = resources;
    }

    void setListener(Listener listener) {
        this.listener = Optional.of(listener);
    }

    void setupContentView(AppCompatActivity activity, VideoAd videoAd) {
        ButterKnife.bind(this, activity.findViewById(android.R.id.content));

        AdUtils.setupCallToActionButton(videoAd, resources, ctaButton);
        videoView.setAspectRatio(videoAd.videoProportion());
        fadingViews = Arrays.asList(ctaButton, shrinkControl, gradient);

        setupClickListeners();
        setActiveUI();
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

    void setPlayState(PlaybackStateTransition transition) {
        playButton.setVisibility(transition.isPaused() || transition.playbackEnded() ? View.VISIBLE : View.GONE);
        loadingIndicator.setVisibility(transition.isBuffering() ? View.VISIBLE : View.GONE);

        if (transition.isPlayerPlaying()) {
            setInactiveUI();
        } else if (transition.isPlayerIdle()) {
            setActiveUI();
        }
    }

    void bindVideoSurface(String uuid, VideoSurfaceProvider.Origin origin) {
        videoSurfaceProvider.setTextureView(uuid, origin, videoView, viewabilityLayer);
    }

    void unbindVideoSurface(VideoSurfaceProvider.Origin origin) {
        videoSurfaceProvider.onDestroy(origin);
    }

    private void setInactiveUI() {
        final Animation fadeOut = AnimationUtils.loadAnimation(videoView.getContext(), R.anim.abc_fade_out);
        fadeOut.setStartOffset(FADE_OUT_OFFSET);
        fadeOut.setDuration(FADE_OUT_DURATION);
        fadeOut.setInterpolator(new AccelerateInterpolator(ACCELLERATOR_FACTOR));

        forEach(fadingViews, view -> {
            view.setAnimation(fadeOut);
            view.setVisibility(View.INVISIBLE);
        });
    }

    private void setActiveUI() {
        forEach(fadingViews, view -> {
            view.clearAnimation();
            view.setVisibility(View.VISIBLE);
        });
    }
}
