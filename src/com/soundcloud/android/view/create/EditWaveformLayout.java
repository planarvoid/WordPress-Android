package com.soundcloud.android.view.create;

import com.soundcloud.android.utils.InputObject;
import com.soundcloud.android.utils.record.RawAudioPlayer;
import com.soundcloud.android.view.TouchLayout;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.ViewConfiguration;

import java.io.File;

public class EditWaveformLayout extends TouchLayout {

    private static final long MIN_SEEK_INTERVAL = 100;
    private static final int UI_UPDATE_SEEK = 1;

    private EditWaveformView mWaveformView;
    private RawAudioPlayer mRawAudioPlayer;

    private long lastSeekTime;
    private long lastSeekX = -1;
    private int touchSlop;

    public EditWaveformLayout(Context context) {
        super(context);
        init();
    }

    public EditWaveformLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditWaveformLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void init() {
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    public EditWaveformView setWaveformFromFile(File f) {
        if (mWaveformView != null) {
            if (mWaveformView.getParent() == this) {
                removeView(mWaveformView);
            }

            mWaveformView.destroy();
            mWaveformView = null;
        }

        mWaveformView = new EditWaveformView(getContext());
        addView(mWaveformView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mWaveformView.setFromFile(f);
        return mWaveformView;
    }

    public void setPlayer(RawAudioPlayer rawAudioPlayer) {
        mRawAudioPlayer = rawAudioPlayer;
    }

    @Override
    protected void processDownInput(InputObject input) {
        seekTouch(input.x);

    }

    @Override
    protected void processMoveInput(InputObject input) {
        seekTouch(input.x);
    }

    @Override
    protected void processUpInput(InputObject input) {
        mTouchHandler.removeMessages(UI_UPDATE_SEEK);
        lastSeekX = -1;
    }

    private void seekTouch(int x) {
        final long now = System.currentTimeMillis();
        if ((lastSeekX == -1 || Math.abs(x - lastSeekX) > touchSlop) && (now - lastSeekTime >= MIN_SEEK_INTERVAL)) {
            lastSeekTime = now;
            lastSeekX = x;
            if (!mTouchHandler.hasMessages(UI_UPDATE_SEEK)) mTouchHandler.sendEmptyMessage(UI_UPDATE_SEEK);
        }
    }

    Handler mTouchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_UPDATE_SEEK:
                    final float seekPercent = ((float) lastSeekX) / getWidth();
                    if (mWaveformView != null) mWaveformView.setCurrentProgress(seekPercent);
                    if (mRawAudioPlayer != null) mRawAudioPlayer.seekTo(seekPercent);
                    break;

                default:
            }
        }
    };


}
