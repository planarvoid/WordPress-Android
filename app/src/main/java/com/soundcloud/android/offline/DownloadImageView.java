package com.soundcloud.android.offline;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.AnimUtils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class DownloadImageView extends ImageView {
    private final Drawable downloading;
    private final Drawable requested;
    private final Drawable downloaded;
    private final Drawable unavailable;
    private DownloadState downloadState;

    public DownloadImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DownloadImageView);
        downloaded = a.getDrawable(R.styleable.DownloadImageView_downloaded);
        downloading = a.getDrawable(R.styleable.DownloadImageView_downloading);
        requested = a.getDrawable(R.styleable.DownloadImageView_requested);
        unavailable = a.getDrawable(R.styleable.DownloadImageView_unavailable);
        a.recycle();
    }

    public boolean isUnavailable() {
        return downloadState == DownloadState.UNAVAILABLE;
    }

    public boolean isRequested() {
        return downloadState == DownloadState.REQUESTED;
    }

    public boolean isDownloading() {
        return downloadState == DownloadState.DOWNLOADING;
    }

    public boolean isDownloaded() {
        return downloadState == DownloadState.DOWNLOADED;
    }

    private void setNoOfflineState() {
        clearAnimation();
        setVisibility(View.GONE);
        setImageDrawable(null);
    }

    private void setDownloadingState() {
        setDownloadStateResource(downloading);
        AnimUtils.runSpinClockwiseAnimationOn(getContext(), this);
    }

    private void setDownloadStateResource(Drawable drawable) {
        clearAnimation();
        setVisibility(View.VISIBLE);
        setImageDrawable(drawable);
    }

    public void setState(DownloadState state) {
        downloadState = state;
        switch (state) {
            case NO_OFFLINE:
                setNoOfflineState();
                break;
            case UNAVAILABLE:
                setDownloadStateResource(unavailable);
                break;
            case REQUESTED:
                setDownloadStateResource(requested);
                break;
            case DOWNLOADING:
                setDownloadingState();
                break;
            case DOWNLOADED:
                setDownloadStateResource(downloaded);
                break;
            default:
                throw new IllegalArgumentException("Unknown state : " + state);
        }
    }
}
