package com.soundcloud.android.view.create;

import com.soundcloud.android.R;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.utils.InputObject;
import com.soundcloud.android.view.TouchLayout;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;

public class CreateWaveDisplay extends TouchLayout {
    public static final int MODE_REC = 0;
    public static final int MODE_PLAYBACK = 1;

    private static final int UI_UPDATE_SEEK = 1;
    private static final int UI_UPDATE_TRIM = 2;
    private static final int UI_ON_TRIM_STATE = 3;

    private static final long TRIM_REPORT_INTERVAL = 200;
    private long mLastTrimAction;

    private int mLeftHandleTouchIndex;
    private int mRightHandleTouchIndex;
    private boolean mSeekMode;

    private int mMode;

    private boolean mIsEditing;

    private CreateWaveView mWaveformView;

    private Rect mWaveformRect;

    private long lastSeekX = -1;
    private final int touchSlop;

    private final TrimHandle rightHandle, leftHandle;
    private Listener mListener;
    private int waveformWidth,leftDragOffsetX, rightDragOffsetX;
    private TrimAction newTrimActionLeft, newTrimActionRight, lastTrimActionLeft, lastTrimActionRight;

    public static interface Listener {
        void onSeek(float pos);
        void onAdjustTrimLeft(float newPos, long moveTimeMs);
        void onAdjustTrimRight(float newPos, long moveTimeMs);
    }

    public CreateWaveDisplay(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        leftHandle = new TrimHandle(getContext(), TrimHandle.HandleType.LEFT);
        rightHandle = new TrimHandle(getContext(), TrimHandle.HandleType.RIGHT);
        mSeekMode = false;
        mLeftHandleTouchIndex = mRightHandleTouchIndex = -1;
        refreshWaveView();
    }

    private CreateWaveView refreshWaveView() {
        if (mWaveformView != null && mWaveformView.getParent() == this) {
            removeView(mWaveformView);
        }

        mWaveformView = new CreateWaveView(getContext());
        LayoutParams viewParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
        viewParams.bottomMargin = (int) getResources().getDimension(R.dimen.create_wave_view_bottom_margin);
        addView(mWaveformView, viewParams);
        return mWaveformView;
    }

    public void setTrimListener(Listener trimListener) {
        mListener = trimListener;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed && getWidth() > 0 && mWaveformView != null && mWaveformView.getWidth() > 0) {
            calcualteWaveformRect();

            leftHandle.getLayoutParams().addRule(RelativeLayout.ALIGN_LEFT, mWaveformView.getId());
            rightHandle.getLayoutParams().addRule(RelativeLayout.ALIGN_RIGHT, mWaveformView.getId());

            // dimension caching
            waveformWidth = mWaveformView.getWidth();
            setTrimHandles();
        }
    }

    @Override
    protected void processDownInput(InputObject input) {
        setTouchMode(input);
        final int x = input.actionIndex == 0 ? input.x : input.pointerX;

        if (mSeekMode) {
            seekTouch(input.x);
        } else if (mLeftHandleTouchIndex > -1 && input.actionIndex == mLeftHandleTouchIndex) {
            leftDragOffsetX = x - leftHandle.getLeft();
            queueUnique(UI_ON_TRIM_STATE);
        } else if (mRightHandleTouchIndex > -1 && input.actionIndex == mRightHandleTouchIndex) {
            rightDragOffsetX = x - rightHandle.getRight();
            queueUnique(UI_ON_TRIM_STATE);
        }
    }

    @Override
    protected void processMoveInput(InputObject input) {
        if (mSeekMode){
               seekTouch(input.x);
        } else {
            if (mLeftHandleTouchIndex > -1){
                newTrimActionLeft = new TrimAction(System.currentTimeMillis(),
                        Math.max(0,Math.min(rightHandle.getLeft() - leftHandle.getWidth(),
                        (mLeftHandleTouchIndex == 0 ? input.x : input.pointerX) - leftDragOffsetX)));

            }

            if (mRightHandleTouchIndex > -1){
                newTrimActionRight = new TrimAction(System.currentTimeMillis(),
                        Math.min(getWidth(), Math.max(leftHandle.getRight() + rightHandle.getWidth(),
                        (mRightHandleTouchIndex == 0 ? input.x : input.pointerX) - rightDragOffsetX)));
            }

            queueTrim(UI_UPDATE_TRIM);
        }

    }

    @Override
    protected void processUpInput(InputObject input) {
        processHandleUpFromPointer(input.actionIndex);
        queueUnique(UI_ON_TRIM_STATE);
        mTouchHandler.removeMessages(UI_UPDATE_SEEK);
        lastSeekX = -1;
        mSeekMode = false;
    }

    @Override
    protected void processPointer1DownInput(InputObject input) {
        setTouchMode(input);
    }

    @Override
    protected void processPointer1UpInput(InputObject input) {
        processHandleUpFromPointer(input.actionIndex);
    }

    private void processHandleUpFromPointer(int pointerIndex){
        if (mLeftHandleTouchIndex == pointerIndex) {
            newTrimActionLeft = null;
            mLeftHandleTouchIndex = -1;
            leftDragOffsetX = 0;
            if (mRightHandleTouchIndex > pointerIndex) mRightHandleTouchIndex--;
        }
        if (mRightHandleTouchIndex == pointerIndex) {
            newTrimActionRight = null;
            mRightHandleTouchIndex = -1;
            rightDragOffsetX = 0;
            if (mLeftHandleTouchIndex > pointerIndex) mLeftHandleTouchIndex--;
        }
        queueTrim(UI_UPDATE_TRIM);
    }

    private void seekTouch(int x) {
        if (x != lastSeekX) {
            lastSeekX = x;
            queueUnique(UI_UPDATE_SEEK);
        }
    }

    protected void queueUnique(int what) {
        if (!mTouchHandler.hasMessages(what)) mTouchHandler.sendEmptyMessage(what);
    }

    protected void queueTrim(int what) {
        if (mLastTrimAction == 0) {
            queueUnique(what);
        } else {
            final long delay = Math.max(0, TRIM_REPORT_INTERVAL - System.currentTimeMillis() - mLastTrimAction);

            if (mTouchHandler.hasMessages(what)) {
                mTouchHandler.removeMessages(what);
            }
            mTouchHandler.sendEmptyMessageDelayed(what, delay);
        }
    }

    private void setTouchMode(InputObject input) {
        if (mMode == MODE_REC || input.actionIndex > 1) return;

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



        final int x = input.actionIndex == 0 ? input.x : input.pointerX;
        final int y = input.actionIndex == 0 ? input.y : input.pointerY;
        if (leftHandleRect != null && leftHandleRect.contains(x,y)) {
            mLeftHandleTouchIndex = input.actionIndex;
        } else if (rightHandleRect != null && rightHandleRect.contains(x,y)) {
            mRightHandleTouchIndex = input.actionIndex;
        } else if (input.action == InputObject.ACTION_TOUCH_DOWN){
            if (mWaveformRect != null && mWaveformRect.contains(x,y)){
                mSeekMode = true;
            }
        }
    }

    private void calcualteWaveformRect(){
        if (mWaveformView != null){
            mWaveformRect = new Rect();
            mWaveformView.getHitRect(mWaveformRect);
        }
    }

    private final Handler mTouchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_UPDATE_SEEK:
                    final float[] trimWindow = SoundRecorder.getInstance(getContext()).getTrimWindow();
                    final int minX = (int) (trimWindow[0] * waveformWidth);
                    final int maxX = (int) (trimWindow[1] * waveformWidth);

                    final float adjustedSeekPosition = Math.min(Math.max(minX, lastSeekX),maxX) - minX;
                    final float seekPercent = adjustedSeekPosition / (maxX - minX);

                    if (mWaveformView != null) mWaveformView.setCurrentProgress(seekPercent);
                    if (mListener != null) {
                        mListener.onSeek(seekPercent);
                    }
                    break;

                case UI_UPDATE_TRIM:
                    mLastTrimAction = System.currentTimeMillis();

                    if (newTrimActionLeft != null && newTrimActionLeft.hasMovedFrom(lastTrimActionLeft)) {
                        leftHandle.update(newTrimActionLeft.position);
                        if (mListener != null) {
                            mListener.onAdjustTrimLeft(Math.max(0, ((float) newTrimActionLeft.position / waveformWidth)),
                                    newTrimActionLeft.timestamp - lastTrimActionLeft.timestamp);
                        }
                        mWaveformView.invalidate();
                    }
                    lastTrimActionLeft = newTrimActionLeft;

                    if (newTrimActionRight != null && newTrimActionRight.hasMovedFrom(lastTrimActionRight)) {
                        rightHandle.update(waveformWidth - newTrimActionRight.position);
                        if (mListener != null) {
                            mListener.onAdjustTrimRight(Math.min(1, ((float) newTrimActionRight.position / waveformWidth)),
                                    newTrimActionRight.timestamp - lastTrimActionRight.timestamp);
                        }
                        mWaveformView.invalidate();
                    }
                    lastTrimActionRight = newTrimActionRight;
                    break;

                case UI_ON_TRIM_STATE:
                    mLastTrimAction = System.currentTimeMillis();
                    lastTrimActionLeft = newTrimActionLeft;
                    lastTrimActionRight = newTrimActionRight;

                    rightHandle.setPressed(mRightHandleTouchIndex != -1);
                    leftHandle.setPressed(mLeftHandleTouchIndex != -1);
                    break;
            }
        }
    };

    private void setTrimHandles() {
        float[] trimWindow = SoundRecorder.getInstance(getContext()).getTrimWindow();
        if (mIsEditing) {
            leftHandle.update((int) (waveformWidth * trimWindow[0]));
            if (leftHandle.getParent() != this) {
                addView(leftHandle);
            }

            rightHandle.update((int) ((1.0d - trimWindow[1]) * waveformWidth));
            if (rightHandle.getParent() != this) {
                addView(rightHandle);
            }
        }
    }

    public void gotoRecordMode() {
        if (mMode != MODE_REC) {
            mMode = MODE_REC;
            mWaveformView.setMode(mMode, true);
        }
    }

    public void gotoPlaybackMode(){
        if (mMode != MODE_PLAYBACK){
            mMode = MODE_PLAYBACK;
            mWaveformView.setMode(mMode, true);
        }
    }

    public void updateAmplitude(float maxAmplitude, boolean isRecording) {
        mWaveformView.updateAmplitude(maxAmplitude, isRecording);
    }

    public void setProgress(float progress) {
        mWaveformView.setPlaybackProgress(progress);
    }

    public void reset() {
        mMode = CreateWaveDisplay.MODE_REC;
        mWaveformView.reset();
    }

    public void setIsEditing(boolean isEditing) {
        if (mIsEditing != isEditing) {
            mIsEditing = isEditing;
            mWaveformView.setIsEditing(isEditing);
            if (mIsEditing) {
                setTrimHandles();
                mWaveformView.setBackgroundColor(Color.BLACK);
            } else {
                if (leftHandle.getParent() == this) removeView(leftHandle);
                if (rightHandle.getParent() == this) removeView(rightHandle);
                mWaveformView.setBackgroundDrawable(null);
            }
        }
    }

    public void onSaveInstanceState(Bundle state) {
        final String prepend = this.getClass().getSimpleName();
        state.putInt(prepend + "_mode", mMode);
        state.putBoolean(prepend + "_inEditMode", mIsEditing);
    }

    public void onRestoreInstanceState(Bundle state) {
        final String prepend = this.getClass().getSimpleName();
        mMode = state.getInt(prepend + "_mode", mMode);
        setIsEditing(state.getBoolean(prepend + "_inEditMode", mIsEditing));
        mWaveformView.setMode(mMode, false);
    }


    private class TrimAction {
        long timestamp;
        int position;

        public TrimAction(long timestamp, int position) {
            this.timestamp = timestamp;
            this.position = position;
        }

        public boolean hasMovedFrom(TrimAction lastTrimActionLeft) {
            return lastTrimActionLeft != null && lastTrimActionLeft.position != position;
        }
    }
}
