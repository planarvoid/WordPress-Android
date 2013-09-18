package com.soundcloud.android.view.play;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.images.ImageOptionsFactory;
import org.jetbrains.annotations.NotNull;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.ref.SoftReference;

public class PlayerArtworkTrackView extends PlayerTrackView {

    private ImageView mArtwork;
    private FrameLayout mArtworkHolder;
    private SoftReference<Drawable> mArtworkBgDrawable;

    private View mArtworkOverlay;

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
//        if (mCurrentArtBindResult == ImageLoader.BindResult.ERROR) {
//            updateArtwork(mOnScreen);
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

    void setTemporaryArtwork(Bitmap bitmap){
        mArtwork.setImageBitmap(bitmap);
        mArtwork.setVisibility(View.VISIBLE);
        mArtworkHolder.setBackgroundDrawable(null);
    }

    void onArtworkSet(boolean animate) {
        if (mArtwork.getVisibility() != View.VISIBLE) { // keep this, presents flashing on second load
            mArtwork.setVisibility(View.VISIBLE);
            if (animate) {
                AnimUtils.runFadeInAnimationOn(getContext(), mArtwork);
                mArtwork.getAnimation().setAnimationListener(new ArtworkFadeInListener(this));
            } else {
                mArtworkHolder.setBackgroundDrawable(null); // take care of overdraw
            }
        }
    }

    void clearBackgroundAfterAnimation(Animation animation){
        if (animation.equals(mArtwork.getAnimation())) {
            mArtworkHolder.setBackgroundDrawable(null); // take care of overdraw
        }
    }

    private void updateArtwork(boolean priority) {
        // this will cause OOMs
        if (mTrack == null || ActivityManager.isUserAMonkey()) return;

        mArtwork.setVisibility(View.GONE);
        mArtworkHolder.setBackgroundResource(R.drawable.artwork_player);
        ImageLoader.getInstance().cancelDisplayTask(mArtwork);

        ImageLoader.getInstance().displayImage(
                mTrack.getPlayerArtworkUri(getContext()),
                mArtwork,
                createPlayerDisplayImageOptions(priority),
                new ArtworkLoadListener(this, mTrack));
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

    private DisplayImageOptions createPlayerDisplayImageOptions(boolean priority){
        return ImageOptionsFactory
                .fullCacheBuilder()
                .delayBeforeLoading(priority ? 0 : 200)
                .displayer(mArtworkDisplayer)
                .build();
    }

    private BitmapDisplayer mArtworkDisplayer = new BitmapDisplayer() {
        @Override
        public Bitmap display(Bitmap bitmap, ImageView imageView, LoadedFrom loadedFrom) {
            imageView.setImageBitmap(bitmap);
            if (mArtwork.getVisibility() != View.VISIBLE) { // keep this, presents flashing on second load
                if (loadedFrom != LoadedFrom.MEMORY_CACHE) {
                    AnimUtils.runFadeInAnimationOn(getContext(), mArtwork);
                    mArtwork.getAnimation().setAnimationListener(new AnimUtils.SimpleAnimationListener() {
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (animation.equals(mArtwork.getAnimation())) {
                                mArtworkHolder.setBackgroundDrawable(null);
                            }
                        }
                    });
                    mArtwork.setVisibility(View.VISIBLE);
                } else {
                    mArtwork.setVisibility(View.VISIBLE);
                    mArtworkHolder.setBackgroundDrawable(null);
                }
            }
            return bitmap;
        }
    };
}
