package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

public class PlayerArtworkView extends FrameLayout {

    private OnWidthChangedListener onWidthChangedListener;

    public PlayerArtworkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.player_artwork_view, this);
    }

    public void setOnWidthChangedListener(OnWidthChangedListener onWidthChangedListener) {
        this.onWidthChangedListener = onWidthChangedListener;
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