package com.soundcloud.android.offline;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class DownloadButtonImageView extends DownloadImageView {

    public DownloadButtonImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDownloadStateResource(queued); // Default button icon
    }

    @Override
    protected void setNoOfflineState() {
        clearAnimation();
        setDownloadStateResource(queued);
    }

    @Override
    protected void setDownloadStateResource(Drawable drawable) {
        clearAnimation();
        setImageDrawable(drawable);
    }

}
