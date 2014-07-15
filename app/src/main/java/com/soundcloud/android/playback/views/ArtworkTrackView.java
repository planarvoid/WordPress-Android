package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.PlayerArtworkLoadListener;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.utils.AnimUtils;
import org.jetbrains.annotations.NotNull;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import java.lang.ref.SoftReference;

public class ArtworkTrackView extends LegacyPlayerTrackView {

    private ImageOperations mImageOperations;
    private ImageView mArtwork;
    private FrameLayout mArtworkHolder;
    private SoftReference<Drawable> mArtworkBgDrawable;

    private View mArtworkOverlay;
    private String mLastArtworkUri;

    private ToggleButton mToggleInfo;
    private ViewFlipper mTrackFlipper;
    private PlayerTrackDetailsLayout mTrackDetailsView;

    private EventBus mEventBus;

    public ArtworkTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mImageOperations = SoundCloudApplication.fromContext(context).getImageOperations();
        mEventBus = SoundCloudApplication.fromContext(context).getEventBus();

        mArtwork = (ImageView) findViewById(R.id.artwork);
        mArtworkHolder = (FrameLayout) mArtwork.getParent();
        mArtworkOverlay = findViewById(R.id.artwork_overlay);
        mArtworkOverlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onCloseCommentMode();
            }
        });
        showDefaultArtwork();

        mTrackFlipper = (ViewFlipper) findViewById(R.id.vfTrackInfo);
        mToggleInfo = (ToggleButton) findViewById(R.id.toggle_info);
        if (mToggleInfo != null) {
            mToggleInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mTrackFlipper != null) {
                        onTrackDetailsFlip(mTrackFlipper, mToggleInfo.isChecked());
                    }
                }
            });
        }
    }

    @Override
    public void setTrackState(PublicApiTrack track, int queuePosition, PlaybackStateProvider playbackStateProvider){
        final boolean changed = !track.equals(this.track);
        super.setTrackState(track, queuePosition, playbackStateProvider);

        updateArtwork(true); //priority is still all wrong

        if (mTrackFlipper != null && changed) {
            onTrackDetailsFlip(mTrackFlipper, false);
        }

        if (mTrackDetailsView != null) {
            mTrackDetailsView.setTrack(track);
        }
    }

    @Override
    public void onDataConnected() {
        super.onDataConnected();
//        if (mCurrentArtBindResult == ImageLoader.BindResult.ERROR) {
//            updateArtwork(onScreen);
//        }
    }

    @Override
    public void clear() {
        super.clear();
        showDefaultArtwork();
    }

    @Override
    protected void onCommentModeChanged(boolean isCommenting, boolean animated) {
        super.onCommentModeChanged(isCommenting, animated);

        if (mTrackFlipper != null && isCommenting) {
            onTrackDetailsFlip(mTrackFlipper, false);
        }

        mArtworkOverlay.clearAnimation();
        if (animated) {
            if (isCommenting) {
                AnimUtils.showView(getContext(), mArtworkOverlay, true);
            } else {
                AnimUtils.hideView(getContext(),mArtworkOverlay,true);
            }
        } else {
            mArtworkOverlay.setVisibility(isCommenting ? VISIBLE : GONE);
        }
    }

    public void setTemporaryArtwork(Bitmap bitmap){
        mArtwork.setImageBitmap(bitmap);
        mArtwork.setVisibility(View.VISIBLE);
        removeArtworkBackground();
    }

    private void removeArtworkBackground() {
        mArtworkHolder.setBackgroundDrawable(null);
    }

    public void onTrackDetailsFlip(@NotNull ViewFlipper trackFlipper, boolean showDetails) {
        if (track != null && showDetails && trackFlipper.getDisplayedChild() == 0) {
            mListener.onCloseCommentMode();

            waveformController.closeComment(false);
            if (mTrackDetailsView == null) {
                mTrackDetailsView = new PlayerTrackDetailsLayout(getContext());
                trackFlipper.addView(mTrackDetailsView);
            }


            mTrackDetailsView.setTrack(track);
            trackFlipper.setInAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_in));
            trackFlipper.setOutAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.hold));
            trackFlipper.showNext();

            mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.PLAYER_INFO.get());

        } else if (!showDetails && trackFlipper.getDisplayedChild() == 1){
            trackFlipper.setInAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.hold));
            trackFlipper.setOutAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.fade_out));
            trackFlipper.showPrevious();
        }
        if (mToggleInfo != null) mToggleInfo.setChecked(showDetails);
    }

    private void updateArtwork(boolean priority) {
        // this will cause OOMs
        if (track == null || ActivityManager.isUserAMonkey()) return;

        showDefaultArtwork(); // during load
        mImageOperations.displayInPlayerView(track.getUrn(), ApiImageSize.getFullImageSize(getResources()),
                mArtwork, mArtworkHolder, priority,
                new PlayerArtworkLoadListener(this, track));
    }

    private void showDefaultArtwork() {
        mArtwork.setVisibility(View.GONE);
        mArtwork.setImageDrawable(null);
        mImageOperations.cancel(mArtwork);

        if (mArtworkBgDrawable == null || mArtworkBgDrawable.get() == null) {
            try {
                mArtworkBgDrawable = new SoftReference<Drawable>(getResources().getDrawable(R.drawable.artwork_player));
            } catch (OutOfMemoryError ignored) {
            }
        }

        final Drawable bg = mArtworkBgDrawable == null ? null : mArtworkBgDrawable.get();
        if (bg == null) {
            mArtwork.setBackgroundColor(0xFFFFFFFF);
        } else {
            mArtworkHolder.setBackgroundDrawable(bg);
        }
    }
}
