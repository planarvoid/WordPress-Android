package com.soundcloud.android.view.play;

import com.soundcloud.android.R;
import com.soundcloud.android.imageloader.ImageLoader;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.images.ImageSize;
import com.soundcloud.android.utils.images.ImageUtils;
import org.jetbrains.annotations.NotNull;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.ref.SoftReference;

public class PlayerArtworkTrackView extends PlayerTrackView {

    private ImageView mArtwork;
    private FrameLayout mArtworkHolder;
    private ImageLoader.BindResult mCurrentArtBindResult;
    private SoftReference<Drawable> mArtworkBgDrawable;

    private View mArtworkOverlay;
    private ImageLoader.Callback mArtworkCallback;

    public PlayerArtworkTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mArtwork = (ImageView) findViewById(R.id.artwork);
        mArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mArtworkHolder = (FrameLayout) mArtwork.getParent();
        mArtworkOverlay = findViewById(R.id.artwork_overlay);
        mArtworkOverlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onCloseCommentMode();
            }
        });

        mArtworkCallback = new ImageLoader.Callback() {
            @Override
            public void onImageError(ImageView view, String url, Throwable error) {
                mCurrentArtBindResult = ImageLoader.BindResult.ERROR;
                Log.e(getClass().getSimpleName(), "Error loading artwork " + error);
            }

            @Override
            public void onImageLoaded(ImageView view, String url) {
                onArtworkSet(mOnScreen);
            }
        };

        showDefaultArtwork();
    }

    @Override
    public void setTrack(@NotNull Track track, int queuePosition, boolean priority) {
        super.setTrack(track, queuePosition, priority);
        updateArtwork(priority);
    }

    @Override
    public void onDataConnected() {
        super.onDataConnected();
        if (mCurrentArtBindResult == ImageLoader.BindResult.ERROR) {
            updateArtwork(mOnScreen);
        }
    }

    @Override
    public void clear() {
        super.clear();
        showDefaultArtwork();
    }

    @Override
    protected void onCommentModeChanged(boolean isCommenting, boolean animated) {
        super.onCommentModeChanged(isCommenting, animated);
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

    private void updateArtwork(boolean postAtFront) {
        // this will cause OOMs
        if (mTrack == null || ActivityManager.isUserAMonkey()) return;

        // TODO, do not change the artwork if it is the same as the last time this was called
        ImageLoader.get(getContext()).unbind(mArtwork);
        if (!mTrack.shouldLoadArtwork()) {
            showDefaultArtwork();
        } else {
            mCurrentArtBindResult = ImageUtils.loadImageSubstitute(
                    getContext(),
                    mArtwork,
                    mTrack.getArtwork(),
                    ImageSize.getPlayerImageSize(getContext()),
                    mArtworkCallback,
                    postAtFront ? ImageLoader.Options.postAtFront() : new ImageLoader.Options());

            if (mCurrentArtBindResult != ImageLoader.BindResult.OK) {
                showDefaultArtwork();
            } else {
                onArtworkSet(false);
            }
        }
    }

    private void showDefaultArtwork() {
        mArtwork.setVisibility(View.GONE);
        mArtwork.setImageDrawable(null);

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

    private void onArtworkSet(boolean animate) {
        if (mArtwork.getVisibility() != View.VISIBLE) { // keep this, presents flashing on second load
            if (animate) {
                AnimUtils.runFadeInAnimationOn(getContext(), mArtwork);
                mArtwork.setVisibility(View.VISIBLE);

                // the listener takes care of overdraw
                mArtwork.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (animation.equals(mArtwork.getAnimation())) mArtworkHolder.setBackgroundDrawable(null);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
            } else {
                mArtwork.setVisibility(View.VISIBLE);
                mArtworkHolder.setBackgroundDrawable(null);
            }
        }
    }
}
