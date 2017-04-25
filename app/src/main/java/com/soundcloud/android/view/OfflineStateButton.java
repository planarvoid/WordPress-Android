package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadButtonImageView;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OfflineStateButton extends LinearLayout {

    private DownloadButtonImageView icon;
    private TextView label;

    public OfflineStateButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public OfflineStateButton(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.offline_state_button, this);
        icon = (DownloadButtonImageView) findViewById(R.id.icon);
        label = (TextView) findViewById(R.id.label);
        setClickable(true);
    }

    public void setState(OfflineState state) {
        icon.setState(state);
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

    public DownloadImageView getIcon() {
        return icon;
    }

    private boolean shouldShowSavingText(OfflineState state) {
        return OfflineState.DOWNLOADING == state
                || OfflineState.REQUESTED == state;
    }

}