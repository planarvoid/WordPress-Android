package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.util.AnimUtils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.View;

public class DownloadImageView extends AppCompatImageView {

    private final Drawable queued;
    private final Drawable downloading;
    private final Drawable downloaded;
    private final Drawable unavailable;

    private OfflineState offlineState;

    public DownloadImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DownloadImageView);
        queued = a.getDrawable(R.styleable.DownloadImageView_queued);
        downloaded = a.getDrawable(R.styleable.DownloadImageView_downloaded);
        downloading = a.getDrawable(R.styleable.DownloadImageView_downloading);
        unavailable = a.getDrawable(R.styleable.DownloadImageView_unavailable);
        a.recycle();
    }

    public boolean isUnavailable() {
        return offlineState == OfflineState.UNAVAILABLE;
    }

    public boolean isRequested() {
        return offlineState == OfflineState.REQUESTED;
    }

    public boolean isDownloading() {
        return offlineState == OfflineState.DOWNLOADING;
    }

    public boolean isDownloaded() {
        return offlineState == OfflineState.DOWNLOADED;
    }

    private void setNoOfflineState() {
        clearAnimation();
        setVisibility(View.GONE);
        setImageDrawable(null);
    }

    private void animateDownloadingState() {
        setDownloadStateResource(downloading);
        AnimUtils.runSpinClockwiseAnimationOn(this);
    }

    private void setDownloadStateResource(Drawable drawable) {
        clearAnimation();
        setVisibility(View.VISIBLE);
        setImageDrawable(drawable);
    }

    public void setState(OfflineState state) {
        offlineState = state;
        switch (state) {
            case NOT_OFFLINE:
                setNoOfflineState();
                break;
            case UNAVAILABLE:
                setDownloadStateResource(unavailable);
                break;
            case REQUESTED:
                setDownloadStateResource(queued);
                break;
            case DOWNLOADING:
                animateDownloadingState();
                break;
            case DOWNLOADED:
                setDownloadStateResource(downloaded);
                break;
            default:
                throw new IllegalArgumentException("Unknown state : " + state);
        }
    }
}
