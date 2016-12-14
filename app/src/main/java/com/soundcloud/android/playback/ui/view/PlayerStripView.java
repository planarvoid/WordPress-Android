package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlayerStripView extends LinearLayout {

    private final TextView castDeviceName;
    private final int expandedHeight;
    private final int collapsedHeight;


    public PlayerStripView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.player_strip, this, true);

        castDeviceName = (TextView) findViewById(R.id.cast_device);
        collapsedHeight = getResources().getDimensionPixelSize(R.dimen.collapsed_player_strip);
        expandedHeight = getResources().getDimensionPixelSize(R.dimen.expanded_player_strip);
    }

    public TextView getCastDeviceName() {
        return castDeviceName;
    }

    public int getCollapsedHeight() {
        return collapsedHeight;
    }

    public int getExpandedHeight() {
        return expandedHeight;
    }

    public boolean isCollapsed() {
        return getLayoutParams().height == collapsedHeight;
    }

    public boolean isExpanded() {
        return getLayoutParams().height == expandedHeight;
    }

}
