package com.soundcloud.android.creators.record;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.InputObject;
import com.soundcloud.android.view.TouchLayout;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ViewConfiguration;
import android.widget.RelativeLayout;

import java.lang.ref.WeakReference;

public class CreateWaveDisplay extends TouchLayout {
    public static final int MODE_REC = 0;
    public static final int MODE_PLAYBACK = 1;

    private static final int UI_UPDATE_SEEK = 1;
    private static final int UI_UPDATE_TRIM = 2;
    private static final int UI_ON_TRIM_STATE = 3;

    private static final long TRIM_REPORT_INTERVAL = 200;
    private final int touchSlop;
    private final TrimHandleView rightHandle, leftHandle;
    private final Handler touchHandler = new TouchHandler(this);
    private long lastTrimAction;
    private int leftHandleTouchIndex = Consts.NOT_SET;
    private int rightHandleTouchIndex = Consts.NOT_SET;
    private boolean seekMode;
    private int mode;
    private boolean isEditing;
    private CreateWaveView waveformView;
    private Rect waveformRect;
    private long lastSeekX = -1;
    private Listener listener;
    private int waveformWidth, leftDragOffsetX, rightDragOffsetX;
    private TrimAction newTrimActionLeft, newTrimActionRight, lastTrimActionLeft, lastTrimActionRight;

    public CreateWaveDisplay(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        leftHandle = new TrimHandleView(getContext(), TrimHandleView.HandleType.LEFT);
        rightHandle = new TrimHandleView(getContext(), TrimHandleView.HandleType.RIGHT);
        seekMode = false;
        leftHandleTouchIndex = rightHandleTouchIndex = -1;
        refreshWaveView();
    }

    public void setTrimListener(Listener trimListener) {
        listener = trimListener;
    }

    public void gotoRecordMode() {
        if (mode != MODE_REC) {
            mode = MODE_REC;
            waveformView.setMode(mode, true);
        }
    }

    public void gotoPlaybackMode(boolean animate) {
        if (mode != MODE_PLAYBACK) {
            mode = MODE_PLAYBACK;
            waveformView.setMode(mode, animate);
        }
    }

    public void updateAmplitude(float maxAmplitude, boolean isRecording) {
        waveformView.updateAmplitude(maxAmplitude, isRecording);
    }

    public void setProgress(float progress) {
        waveformView.setPlaybackProgress(progress);
    }

    public void reset() {
        mode = CreateWaveDisplay.MODE_REC;
        waveformView.reset();
    }

    public void setIsEditing(boolean isEditing) {
        if (this.isEditing != isEditing) {
            this.isEditing = isEditing;
            waveformView.setIsEditing(isEditing);
            if (this.isEditing) {
                setTrimHandles();
            } else {
                if (leftHandle.getParent() == this) {
                    removeView(leftHandle);
                }
                if (rightHandle.getParent() == this) {
                    removeView(rightHandle);
                }
            }
        }
    }

    public void onSaveInstanceState(Bundle state) {
        final String prepend = this.getClass().getSimpleName();
        state.putInt(prepend + "_mode", mode);
        state.putBoolean(prepend + "_inEditMode", isEditing);
    }

    public void onRestoreInstanceState(Bundle state) {
        final String prepend = this.getClass().getSimpleName();
        mode = state.getInt(prepend + "_mode", mode);
        setIsEditing(state.getBoolean(prepend + "_inEditMode", isEditing));
        waveformView.setMode(mode, false);
    }

    public void onDestroy() {
        super.onDestroy();
        waveformView.onDestroy();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (changed && getWidth() > 0 && waveformView != null && waveformView.getWidth() > 0) {
            calculateWaveformRect();

            leftHandle.getLayoutParams().addRule(RelativeLayout.ALIGN_LEFT, waveformView.getId());
            rightHandle.getLayoutParams().addRule(RelativeLayout.ALIGN_RIGHT, waveformView.getId());

            // dimension caching
            waveformWidth = waveformView.getWidth();
            setTrimHandles();
        }
    }

    @Override
    protected void processDownInput(InputObject input) {
        setTouchMode(input);

        if (seekMode) {
            seekTouch(input.x);
        } else if (leftHandleTouchIndex > -1 && input.actionIndex == leftHandleTouchIndex) {
            queueUnique(UI_ON_TRIM_STATE);
        } else if (rightHandleTouchIndex > -1 && input.actionIndex == rightHandleTouchIndex) {
            queueUnique(UI_ON_TRIM_STATE);
        }
    }

    @Override
    protected void processMoveInput(InputObject input) {
        if (seekMode) {
            seekTouch(input.x);
        } else {
            final int x = input.actionIndex == 0 ? input.x : input.pointerX;
            if (leftHandleTouchIndex > -1) {
                if (leftDragOffsetX == Consts.NOT_SET){
                    leftDragOffsetX = x - leftHandle.getLeft();
                }
                newTrimActionLeft = new TrimAction(System.currentTimeMillis(),
                        Math.max(0, Math.min(rightHandle.getLeft() - leftHandle.getWidth(),
                                (leftHandleTouchIndex == 0 ? input.x : input.pointerX) - leftDragOffsetX)));

            }

            if (rightHandleTouchIndex > -1) {
                if (rightDragOffsetX == Consts.NOT_SET){
                    rightDragOffsetX = x - rightHandle.getRight();
                }
                newTrimActionRight = new TrimAction(System.currentTimeMillis(),
                        Math.min(getWidth(), Math.max(leftHandle.getRight() + rightHandle.getWidth(),
                                (rightHandleTouchIndex == 0 ? input.x : input.pointerX) - rightDragOffsetX)));
            }

            queueTrim(UI_UPDATE_TRIM);
        }

    }

    @Override
    protected void processUpInput(InputObject input) {
        processHandleUpFromPointer(input.actionIndex);
        queueUnique(UI_ON_TRIM_STATE);
        touchHandler.removeMessages(UI_UPDATE_SEEK);
        lastSeekX = -1;
        seekMode = false;
    }

    @Override
    protected void processPointer1DownInput(InputObject input) {
        setTouchMode(input);
    }

    @Override
    protected void processPointer1UpInput(InputObject input) {
        processHandleUpFromPointer(input.actionIndex);
    }

    protected void queueUnique(int what) {
        if (!touchHandler.hasMessages(what)) {
            touchHandler.sendEmptyMessage(what);
        }
    }

    protected void queueTrim(int what) {
        if (lastTrimAction == 0) {
            queueUnique(what);
        } else {
            final long delay = Math.max(0, TRIM_REPORT_INTERVAL - System.currentTimeMillis() - lastTrimAction);

            if (touchHandler.hasMessages(what)) {
                touchHandler.removeMessages(what);
            }
            touchHandler.sendEmptyMessageDelayed(what, delay);
        }
    }

    private CreateWaveView refreshWaveView() {
        if (waveformView != null && waveformView.getParent() == this) {
            removeView(waveformView);
        }

        waveformView = new CreateWaveView(getContext());
        LayoutParams viewParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        viewParams.bottomMargin = (int) getResources().getDimension(R.dimen.create_wave_view_bottom_margin);
        addView(waveformView, viewParams);
        return waveformView;
    }

    private void processHandleUpFromPointer(int pointerIndex) {
        if (leftHandleTouchIndex == pointerIndex) {
            newTrimActionLeft = null;
            leftHandleTouchIndex = Consts.NOT_SET;
            leftDragOffsetX = Consts.NOT_SET;
            if (rightHandleTouchIndex > pointerIndex) {
                rightHandleTouchIndex--;
            }
        }
        if (rightHandleTouchIndex == pointerIndex) {
            newTrimActionRight = null;
            rightHandleTouchIndex = Consts.NOT_SET;
            rightDragOffsetX = Consts.NOT_SET;
            if (leftHandleTouchIndex > pointerIndex) {
                leftHandleTouchIndex--;
            }
        }
        queueTrim(UI_UPDATE_TRIM);
    }

    private void seekTouch(int x) {
        if (x != lastSeekX) {
            lastSeekX = x;
            queueUnique(UI_UPDATE_SEEK);
        }
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private void setTouchMode(InputObject input) {
        if (mode == MODE_REC || input.actionIndex > 1) {
            return;
        }

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
        if (leftHandleRect != null && leftHandleRect.contains(x, y)) {
            leftHandleTouchIndex = input.actionIndex;
        } else if (rightHandleRect != null && rightHandleRect.contains(x, y)) {
            rightHandleTouchIndex = input.actionIndex;
        } else if (input.action == InputObject.ACTION_TOUCH_DOWN) {
            if (waveformRect != null && waveformRect.contains(x, y)) {
                seekMode = true;
            }
        }
    }

    private void calculateWaveformRect() {
        if (waveformView != null) {
            waveformRect = new Rect();
            waveformView.getHitRect(waveformRect);
        }
    }

    private void setTrimHandles() {
        float[] trimWindow = SoundRecorder.getInstance(getContext()).getTrimWindow();
        if (isEditing) {
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

    public interface Listener {
        void onSeek(float pos);

        void onAdjustTrimLeft(float newPos, long moveTimeMs);

        void onAdjustTrimRight(float newPos, long moveTimeMs);
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private static final class TouchHandler extends Handler {
        private final WeakReference<CreateWaveDisplay> viewRef;

        private TouchHandler(CreateWaveDisplay view) {
            this.viewRef = new WeakReference<>(view);
        }

        @Override @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
        public void handleMessage(Message msg) {
            final CreateWaveDisplay view = viewRef.get();
            if (view == null) {
                return;
            }
            switch (msg.what) {
                case UI_UPDATE_SEEK:
                    final float[] trimWindow = SoundRecorder.getInstance(view.getContext()).getTrimWindow();
                    final int minX = view.isEditing ? (int) (trimWindow[0] * view.waveformWidth) : 0;
                    final int maxX = view.isEditing ? (int) (trimWindow[1] * view.waveformWidth) : view.waveformWidth;

                    final float adjustedSeekPosition = Math.min(Math.max(minX, view.lastSeekX), maxX) - minX;
                    final float seekPercent = adjustedSeekPosition / (maxX - minX);

                    if (view.listener != null) {
                        view.listener.onSeek(seekPercent);
                    }
                    break;

                case UI_UPDATE_TRIM:
                    view.lastTrimAction = System.currentTimeMillis();

                    if (view.newTrimActionLeft != null && view.newTrimActionLeft.hasMovedFrom(view.lastTrimActionLeft)) {
                        view.leftHandle.update(view.newTrimActionLeft.position);
                        if (view.listener != null) {
                            view.listener.onAdjustTrimLeft(Math.max(0, ((float) view.newTrimActionLeft.position / view.waveformWidth)),
                                    view.newTrimActionLeft.timestamp - view.lastTrimActionLeft.timestamp);
                        }
                        view.waveformView.invalidate();
                    }
                    view.lastTrimActionLeft = view.newTrimActionLeft;

                    if (view.newTrimActionRight != null && view.newTrimActionRight.hasMovedFrom(view.lastTrimActionRight)) {
                        view.rightHandle.update(view.waveformWidth - view.newTrimActionRight.position);
                        if (view.listener != null) {
                            view.listener.onAdjustTrimRight(Math.min(1, ((float) view.newTrimActionRight.position / view.waveformWidth)),
                                    view.newTrimActionRight.timestamp - view.lastTrimActionRight.timestamp);
                        }
                        view.waveformView.invalidate();
                    }
                    view.lastTrimActionRight = view.newTrimActionRight;
                    break;

                case UI_ON_TRIM_STATE:
                    view.lastTrimAction = System.currentTimeMillis();
                    view.lastTrimActionLeft = view.newTrimActionLeft;
                    view.lastTrimActionRight = view.newTrimActionRight;

                    view.rightHandle.setPressed(view.rightHandleTouchIndex != -1);
                    view.leftHandle.setPressed(view.leftHandleTouchIndex != -1);
                    break;
            }
        }
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
