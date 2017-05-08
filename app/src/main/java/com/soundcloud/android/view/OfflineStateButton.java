package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.util.AnimUtils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OfflineStateButton extends LinearLayout {

    private ImageView icon;
    private TextView label;

    private final Drawable defaultIcon;
    private final Drawable downloadingIcon;
    private final Drawable downloadedIcon;
    private final Drawable waitingIcon;

    private OfflineState offlineState;

    public OfflineStateButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.OfflineStateButton);
        defaultIcon = a.getDrawable(R.styleable.OfflineStateButton_defaultIcon);
        downloadingIcon = a.getDrawable(R.styleable.OfflineStateButton_downloadingIcon);
        downloadedIcon = a.getDrawable(R.styleable.OfflineStateButton_downloadedIcon);
        waitingIcon = a.getDrawable(R.styleable.OfflineStateButton_waitingIcon);
        a.recycle();

        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.offline_state_button, this);
        icon = (ImageView) findViewById(R.id.icon);
        label = (TextView) findViewById(R.id.label);
        setClickable(true);
        setState(OfflineState.NOT_OFFLINE);
    }

    @VisibleForTesting
    public boolean isDefaultState() {
        return OfflineState.REQUESTED == offlineState || OfflineState.NOT_OFFLINE == offlineState;
    }

    @VisibleForTesting
    public boolean isWaitingState() {
        return offlineState == OfflineState.UNAVAILABLE;
    }

    @VisibleForTesting
    public boolean isDownloadingState() {
        return offlineState == OfflineState.DOWNLOADING;
    }

    @VisibleForTesting
    public boolean isDownloadedState() {
        return offlineState == OfflineState.DOWNLOADED;
    }

    public void setState(OfflineState state) {
        if (!isTransitioningFromDownloadingToRequested(state)) {
            offlineState = state;
            setIcon(state);
            setLabel(state);
        }
    }

    private boolean isTransitioningFromDownloadingToRequested(OfflineState state) {
        return OfflineState.DOWNLOADING == offlineState && OfflineState.REQUESTED == state;
    }

    private void setIcon(OfflineState state) {
        switch (state) {
            case NOT_OFFLINE:
            case REQUESTED:
                setDownloadStateResource(defaultIcon);
                break;
            case UNAVAILABLE:
                setDownloadStateResource(waitingIcon);
                break;
            case DOWNLOADING:
                setDownloadStateResource(downloadingIcon);
                AnimUtils.runSpinClockwiseAnimationOn(icon);
                break;
            case DOWNLOADED:
                setDownloadStateResource(downloadedIcon);
                break;
            default:
                throw new IllegalArgumentException("Unknown state : " + state);
        }
    }

    private void setDownloadStateResource(Drawable drawable) {
        icon.clearAnimation();
        icon.setImageDrawable(drawable);
    }

    private void setLabel(OfflineState state) {
        if (shouldShowSavingText(state)) {
            label.setVisibility(VISIBLE);
            label.setText(R.string.offline_update_in_progress);
        } else {
            label.setVisibility(GONE);
        }
    }

    public void showNoWiFi() {
        setState(OfflineState.UNAVAILABLE);
        label.setVisibility(VISIBLE);
        label.setText(R.string.offline_no_wifi);
    }

    public void showNoConnection() {
        setState(OfflineState.UNAVAILABLE);
    }

    private boolean shouldShowSavingText(OfflineState state) {
        return OfflineState.DOWNLOADING == state || OfflineState.REQUESTED == state;
    }

}
