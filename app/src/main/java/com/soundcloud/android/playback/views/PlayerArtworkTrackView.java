package com.soundcloud.android.playback.views;

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
import android.text.TextUtils;
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
    private String mLastArtworkUri;

    public PlayerArtworkTrackView(Context context) {
        this(context, null);
    }

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
    protected void setTrackInternal(@NotNull Track track, boolean priority) {
        super.setTrackInternal(track, priority);
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
        removeArtworkBackground();
    }

    private void removeArtworkBackground() {
        mArtworkHolder.setBackgroundDrawable(null);
    }

    void onArtworkSet(boolean animate) {
        if (mArtwork.getVisibility() != View.VISIBLE) { // keep this, presents flashing on second load
            mArtwork.setVisibility(View.VISIBLE);
            if (animate) {
                AnimUtils.runFadeInAnimationOn(getContext(), mArtwork);
                mArtwork.getAnimation().setAnimationListener(new ArtworkFadeInListener(this));
            } else {
                removeArtworkBackground();
            }
        }
    }

    void clearBackgroundAfterAnimation(Animation animation){
        if (animation.equals(mArtwork.getAnimation())) {
            removeArtworkBackground();
        }
    }

    private void updateArtwork(boolean priority) {
        // this will cause OOMs
        if (mTrack == null || ActivityManager.isUserAMonkey()) return;

        final String playerArtworkUri = mTrack.getPlayerArtworkUri(getContext());
        if (mLastArtworkUri == null || !mLastArtworkUri.equals(playerArtworkUri)){
            mLastArtworkUri = playerArtworkUri;

            showDefaultArtwork(); // during load
            if (!TextUtils.isEmpty(playerArtworkUri)){
                ImageLoader.getInstance().displayImage(
                        playerArtworkUri,
                        mArtwork,
                        createPlayerDisplayImageOptions(priority),
                        new ArtworkLoadListener(this, mTrack));
            }
        }
    }

    private void showDefaultArtwork() {
        mArtwork.setVisibility(View.GONE);
        mArtwork.setImageDrawable(null);
        ImageLoader.getInstance().cancelDisplayTask(mArtwork);

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
