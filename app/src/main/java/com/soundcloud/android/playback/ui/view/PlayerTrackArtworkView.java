package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PlayerTrackArtworkView extends FrameLayout {
    private static final float PLAYING_SCALE = 1.03F;
    private OnWidthChangedListener onWidthChangedListener;

    private final ImageView wrappedImageView;
    private final ImageView imageOverlay;
    private final View imageHolder;

    public PlayerTrackArtworkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.player_track_artwork_view, this);

        wrappedImageView = (ImageView) findViewById(R.id.artwork_image_view);
        imageOverlay = (ImageView) findViewById(R.id.artwork_overlay_image);
        imageHolder = findViewById(R.id.artwork_holder);

        wrappedImageView.setScaleX(PLAYING_SCALE);
        wrappedImageView.setScaleY(PLAYING_SCALE);
    }

    public void setOnWidthChangedListener(OnWidthChangedListener onWidthChangedListener) {
        this.onWidthChangedListener = onWidthChangedListener;
    }

    public View getArtworkHolder() {
        return imageHolder;
    }

    public void setArtworkActive(boolean isActive) {
        if (isActive){
            wrappedImageView.animate()
                    .setDuration(100)
                    .setInterpolator(new DecelerateInterpolator())
                    .scaleX(PLAYING_SCALE)
                    .scaleY(PLAYING_SCALE)
                    .start();
        } else {
            wrappedImageView.animate()
                    .setDuration(100)
                    .scaleX(1)
                    .scaleY(1)
                    .start();
        }
    }

    public ImageView getWrappedImageView() {
        return wrappedImageView;
    }

    public ImageView getImageOverlay() {
        return imageOverlay;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (onWidthChangedListener != null){
            onWidthChangedListener.onArtworkSizeChanged();
        }
    }

    public interface OnWidthChangedListener{
        void onArtworkSizeChanged();
    }
}
