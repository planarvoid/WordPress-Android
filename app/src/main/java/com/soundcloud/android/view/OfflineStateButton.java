package com.soundcloud.android.view;

import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OfflineStateButton extends LinearLayout {

    private DownloadImageView icon;
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
        icon = (DownloadImageView) findViewById(R.id.icon);
        label = (TextView) findViewById(R.id.label);
        setClickable(true);
        icon.setState(OfflineState.REQUESTED, true); // Default icon for upsell
    }

    public void setState(OfflineState state) {
        label.setVisibility(shouldShowText(state)
                            ? View.VISIBLE
                            : View.GONE);
        icon.setState(OfflineState.NOT_OFFLINE != state
                      ? state
                      : OfflineState.REQUESTED, true);
    }

    public DownloadImageView getIcon() {
        return icon;
    }

    private boolean shouldShowText(OfflineState state) {
        return OfflineState.DOWNLOADING == state
                || OfflineState.REQUESTED == state;
    }

}
