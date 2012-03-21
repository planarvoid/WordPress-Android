package com.soundcloud.android.view.create;

import com.soundcloud.android.utils.InputObject;
import com.soundcloud.android.utils.record.RawAudioPlayer;
import com.soundcloud.android.view.TouchLayout;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;

public class CreateWaveDisplay extends TouchLayout implements CreateWaveView.TransitionListener{

    private static long ANIMATION_ZOOM_TIME = 500;

    public static final int MODE_GAUGE_IDLE = 0;
    public static final int MODE_GAUGE_REC = 1;
    public static final int MODE_PLAYBACK = 2;

    private static final long MIN_SEEK_INTERVAL = 100;
    private static final int UI_UPDATE_SEEK = 1;
    private static final int UI_UPDATE_TRIM_RIGHT = 2;
    private static final int UI_UPDATE_TRIM_LEFT = 3;

    static final int TOUCH_MODE_NONE = 0;
    static final int TOUCH_MODE_SEEK_DRAG = 1;
    static final int TOUCH_MODE_RIGHT_DRAG = 2;
    static final int TOUCH_MODE_LEFT_DRAG = 3;

    private int mMode;
    private int mTouchMode;

    private boolean mInEditMode;

    private CreateWaveView mWaveformView;
    private RawAudioPlayer mRawAudioPlayer;

    private Rect mWaveformRect;

    private long lastSeekTime;
    private long lastTouchX = -1;
    private int touchSlop;

    private View rightHandle, leftHandle;
    private LayoutParams rightLp, leftLp;
    private Listener mListener;

    private float trimPercentLeft, trimPercentRight;
    private int waveformWidth;
    private int dragOffsetX;

    public static interface Listener {
        void onSeek(float pos);
        void onAdjustTrimLeft(float pos);
        void onAdjustTrimRight(float pos);
    }

    public CreateWaveDisplay(Context context) {
        super(context);
        init();
    }

    public CreateWaveDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CreateWaveDisplay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {

        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        final float density = getContext().getResources().getDisplayMetrics().density;
        final int dim = (int) (30 * density);

        leftHandle = new View(getContext());
        leftHandle.setBackgroundColor(Color.GRAY);
        leftLp = new LayoutParams(dim,dim);
        leftLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,1);
        leftLp.addRule(RelativeLayout.ALIGN_PARENT_LEFT,1);
        trimPercentLeft = 0.0f;

        rightHandle = new View(getContext());
        rightHandle.setBackgroundColor(Color.GRAY);
        rightLp = new LayoutParams(dim,dim);
        rightLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,1);
        rightLp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,1);
        trimPercentRight = 1.0f;

        mTouchMode = TOUCH_MODE_NONE;

        refreshWaveView();
    }



    public CreateWaveView refreshWaveView() {
        if (mWaveformView != null && mWaveformView.getParent() == this) {
            removeView(mWaveformView);
        }

        mWaveformView = new CreateWaveView(getContext(), this);

        LayoutParams viewParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
        //viewParams.rightMargin = viewParams.leftMargin = (int) (getContext().getResources().getDisplayMetrics().density * 15);
        viewParams.bottomMargin = (int) (getContext().getResources().getDisplayMetrics().density * 15);
        addView(mWaveformView, viewParams);
        return mWaveformView;
    }

    public void setTrimListener(Listener trimListener) {
        mListener = trimListener;
    }

    @Override
    public void onFull() {
    }

    @Override
    public void onZoom() {
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed && getWidth() > 0 && mWaveformView != null && mWaveformView.getWidth() > 0) {
            calcualteWaveformRect();

            leftLp.addRule(RelativeLayout.ALIGN_LEFT,mWaveformView.getId());
            rightLp.addRule(RelativeLayout.ALIGN_RIGHT,mWaveformView.getId());

            // dimension caching
            waveformWidth = mWaveformView.getWidth();


            //mListener.onWaveWidth(waveformWidth);
        }
    }

    @Override
    protected void processDownInput(InputObject input) {
        switch (setTouchMode(input)){
            case TOUCH_MODE_SEEK_DRAG :
                seekTouch(input.x);
                break;
            case TOUCH_MODE_LEFT_DRAG :
                dragOffsetX = input.x - leftHandle.getLeft();
                break;
            case TOUCH_MODE_RIGHT_DRAG :
                dragOffsetX = input.x - rightHandle.getRight();
                break;
        }
    }



    @Override
    protected void processMoveInput(InputObject input) {
        switch(mTouchMode){
            case TOUCH_MODE_SEEK_DRAG :
                seekTouch(input.x);
                break;
            case TOUCH_MODE_LEFT_DRAG :
                lastTouchX = Math.max(0,Math.min(rightHandle.getLeft() - leftHandle.getWidth(),input.x - dragOffsetX));
                queueUnique(UI_UPDATE_TRIM_LEFT);
                break;
            case TOUCH_MODE_RIGHT_DRAG :
                lastTouchX = Math.min(getWidth(),Math.max(leftHandle.getRight() + rightHandle.getWidth(), input.x - dragOffsetX));
                queueUnique(UI_UPDATE_TRIM_RIGHT);
                break;
        }
    }

    @Override
    protected void processUpInput(InputObject input) {
        mTouchHandler.removeMessages(UI_UPDATE_SEEK);
        lastTouchX = -1;
        dragOffsetX = 0;
        mTouchMode = TOUCH_MODE_NONE;
    }

    private void seekTouch(int x) {
        if ((lastTouchX == -1 || Math.abs(x - lastTouchX) > touchSlop)) {
            lastTouchX = x;
            queueUnique(UI_UPDATE_SEEK);
        }
    }

    protected void queueUnique(int what) {
        if (!mTouchHandler.hasMessages(what)) mTouchHandler.sendEmptyMessage(what);
    }

    private int setTouchMode(InputObject input) {
        Rect leftHandleRect = null, rightHandleRect = null;
        if (leftHandle.getParent() == this) {
            leftHandleRect = new Rect();
            leftHandle.getHitRect(leftHandleRect);
            leftHandleRect.set(leftHandleRect.left - touchSlop,
                                leftHandleRect.top - touchSlop,
                                leftHandleRect.right, // prevent overlapping
                                leftHandleRect.bottom + touchSlop);
        }
        if (rightHandle.getParent() == this) {
            rightHandleRect = new Rect();
            rightHandle.getHitRect(rightHandleRect);
            rightHandleRect.set(rightHandleRect.left, // prevent overlapping
                                rightHandleRect.top - touchSlop,
                                rightHandleRect.right + touchSlop,
                                rightHandleRect.bottom + touchSlop);
        }

        if (leftHandleRect != null && leftHandleRect.contains(input.x,input.y)) {
            mTouchMode = TOUCH_MODE_LEFT_DRAG;
        } else if (rightHandleRect != null && rightHandleRect.contains(input.x,input.y)) {
            mTouchMode = TOUCH_MODE_RIGHT_DRAG;
        } else if (mWaveformRect != null && mWaveformRect.contains(input.x,input.y)){
            mTouchMode = TOUCH_MODE_SEEK_DRAG;
        } else {
            mTouchMode = TOUCH_MODE_NONE;
        }
        return mTouchMode;
    }



    private void calcualteWaveformRect(){
        if (mWaveformView != null){
            mWaveformRect = new Rect();
            mWaveformView.getHitRect(mWaveformRect);
        }
    }

    Handler mTouchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_UPDATE_SEEK:
                    final float seekPercent = ((float) lastTouchX) / waveformWidth;
                    if (mWaveformView != null) mWaveformView.setCurrentProgress(seekPercent);
                    if (mListener != null) {
                        mListener.onSeek(seekPercent);
                    }
                    break;

                case UI_UPDATE_TRIM_LEFT:
                    leftLp.leftMargin = (int) lastTouchX;
                    mWaveformView.setTrimLeft((int) lastTouchX);
                    trimPercentLeft = Math.max(0,((float) lastTouchX  / waveformWidth));
                    leftHandle.requestLayout();
                    if (mListener != null) {
                        mListener.onAdjustTrimLeft(trimPercentLeft);
                    }
                    break;

                case UI_UPDATE_TRIM_RIGHT:
                    mWaveformView.setTrimRight((int) lastTouchX);
                    rightLp.rightMargin = (waveformWidth - (int) lastTouchX);
                    trimPercentRight = Math.min(1, ((float) lastTouchX / waveformWidth));
                    rightHandle.requestLayout();
                    if (mListener != null) {
                        mListener.onAdjustTrimRight(trimPercentRight);
                    }
                    break;

                default:
            }
        }
    };

    public void setTrimHandles() {
        leftLp.leftMargin = (int) (waveformWidth * trimPercentLeft);
        if (leftHandle.getParent() != this){
            addView(leftHandle, leftLp);
        }

        rightLp.rightMargin = (int) ((1.0f - trimPercentRight) * waveformWidth);
        if (rightHandle.getParent() != this){
            addView(rightHandle, rightLp);
        }
    }

    public void gotoRecordMode() {
        mMode = MODE_GAUGE_REC;
        mWaveformView.gotoRecordMode();
    }

    public void gotoPlaybackMode(){
        mMode = MODE_PLAYBACK;
        mWaveformView.gotoPlaybackMode();
    }

    public void updateAmplitude(float maxAmplitude, boolean isRecording) {
        mWaveformView.updateAmplitude(maxAmplitude, isRecording);
    }

    public void setProgress(float progress) {
        mWaveformView.setPlaybackProgress(progress);
    }

    public void reset() {
        mWaveformView.reset();
    }

    public void setInEditMode(boolean inEditMode) {
        if (inEditMode != mInEditMode){
            mInEditMode = inEditMode;
            mWaveformView.setInEditMode(inEditMode);
            if (mInEditMode){
                setTrimHandles();
                mWaveformView.setBackgroundColor(Color.BLACK);
            } else {
                if (leftHandle.getParent() == this) removeView(leftHandle);
                if (rightHandle.getParent() == this) removeView(rightHandle);
                mWaveformView.setBackgroundDrawable(null);
            }
        }
    }
}
