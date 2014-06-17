package com.soundcloud.android.playback.ui.progress;

import org.jetbrains.annotations.Nullable;

import android.view.View;

public abstract class ProgressHelper {

    private final int startPosition;
    private final int endPosition;

    protected ProgressHelper(int startPosition, int endPosition) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    public abstract void setValueFromProportion(View progressView, float value);
    public abstract @Nullable ProgressAnimator createAnimator(View progressView, float startProportion);

    public int getEndPosition() {
        return endPosition;
    }

    protected float getValueFromProportion(float proportion){
        return startPosition + ((endPosition - startPosition) * proportion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            ProgressHelper that = (ProgressHelper) o;
            return endPosition == that.endPosition && startPosition == that.startPosition;
        }
    }

    @Override
    public int hashCode() {
        int result = startPosition;
        result = 31 * result + endPosition;
        return result;
    }
}
