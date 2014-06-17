package com.soundcloud.android.playback.ui.progress;

import org.jetbrains.annotations.Nullable;

import android.view.View;

public abstract class ProgressHelper {
    private int startPosition;
    private int endPosition;

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProgressHelper that = (ProgressHelper) o;

        if (endPosition != that.endPosition) return false;
        if (startPosition != that.startPosition) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = startPosition;
        result = 31 * result + endPosition;
        return result;
    }
}
