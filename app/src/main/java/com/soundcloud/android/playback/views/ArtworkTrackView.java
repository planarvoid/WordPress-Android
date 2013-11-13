package com.soundcloud.android.playback.views;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.CloudPlaybackService;
import com.soundcloud.android.tracking.Page;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.utils.images.ImageOptionsFactory;
import org.jetbrains.annotations.NotNull;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ToggleButton;
import android.widget.ViewFlipper;

import java.lang.ref.SoftReference;

public class ArtworkTrackView extends PlayerTrackView {

    private ImageView mArtwork;
    private FrameLayout mArtworkHolder;
    private SoftReference<Drawable> mArtworkBgDrawable;

    private View mArtworkOverlay;
    private String mLastArtworkUri;

    private ToggleButton mToggleInfo;
    private ViewFlipper mTrackFlipper;
    private PlayerTrackDetailsLayout mTrackDetailsView;

    public ArtworkTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);

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
    protected void setTrackInternal(@NotNull Track track, boolean priority) {
        super.setTrackInternal(track, priority);
        updateArtwork(priority);

        if (mTrackFlipper != null && !track.equals(mTrack)) {
            onTrackDetailsFlip(mTrackFlipper, false);
        }
        if (mTrackDetailsView != null) {
            mTrackDetailsView.setTrack(mTrack);
        }
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

    public void onTrackDetailsFlip(@NotNull ViewFlipper trackFlipper, boolean showDetails) {
        if (mTrack != null && showDetails && trackFlipper.getDisplayedChild() == 0) {
            mListener.onCloseCommentMode();

            SoundCloudApplication.fromContext(getContext()).track(Page.Sounds_info__main, mTrack);
            mWaveformController.closeComment(false);
            if (mTrackDetailsView == null) {
                mTrackDetailsView = new PlayerTrackDetailsLayout(getContext());
                trackFlipper.addView(mTrackDetailsView);
            }

            // according to this logic, we will only load the info if we haven't yet or there was an error
            // there is currently no manual or stale refresh logic
            if (mTrack.shouldLoadInfo()) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        final Context context = getContext();
                        if (context != null){
                            context.startService(new Intent(CloudPlaybackService.Actions.LOAD_TRACK_INFO).putExtra(Track.EXTRA_ID, mTrack.getId()));
                        }
                    }
                }, 400); //flipper animation time is 250, so this should be enough to allow the animation to end

                mTrackDetailsView.setTrack(mTrack, true);
            } else {
                mTrackDetailsView.setTrack(mTrack);
            }

            trackFlipper.setInAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fade_in));
            trackFlipper.setOutAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.hold));
            trackFlipper.showNext();
        } else if (!showDetails && trackFlipper.getDisplayedChild() == 1){
            trackFlipper.setInAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.hold));
            trackFlipper.setOutAnimation(AnimationUtils.loadAnimation(getContext(),R.anim.fade_out));
            trackFlipper.showPrevious();
        }
        if (mToggleInfo != null) mToggleInfo.setChecked(showDetails);
    }

    @Override
    protected void onTrackInfoChanged() {
        if (mTrackDetailsView != null) {
            mTrackDetailsView.setTrack(mTrack);
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
