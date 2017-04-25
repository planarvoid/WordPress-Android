package com.soundcloud.android.offline;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class DownloadButtonImageView extends DownloadImageView {

    public DownloadButtonImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setState(OfflineState.REQUESTED); // Default button icon
    }

    @Override
    protected void setNoOfflineState() {
        clearAnimation();
        setState(OfflineState.REQUESTED);
    }

    @Override
    protected void setDownloadStateResource(Drawable drawable) {
        clearAnimation();
        setImageDrawable(drawable);
    }

}
