package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;

import android.content.Context;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PlayerUpsellView extends RelativeLayout {

    private final TextView upsellText;
    private final Button upsellButton;

    private final int expandedHeight;
    private final int collapsedHeight;

    public PlayerUpsellView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.player_upsell, this, true);

        upsellText = findViewById(R.id.upsell_text);
        upsellButton = findViewById(R.id.upsell_button);
        expandedHeight = getResources().getDimensionPixelSize(R.dimen.player_upsell_height);
        collapsedHeight = 0;
    }

    public void showUpsell(@StringRes int upsellButtonTextRes, boolean isCollapsed) {
        upsellButton.setText(upsellButtonTextRes);

        if (isCollapsed) {
            setCollapsed();
        } else {
            setExpanded();
        }
        setVisibility(VISIBLE);
    }

    public Button getUpsellButton() {
        return upsellButton;
    }

    public boolean isVisible() {
        return getVisibility() == View.VISIBLE;
    }

    private void setCollapsed() {
        getLayoutParams().height = collapsedHeight;
        requestLayout();
    }

    private void setExpanded() {
        getLayoutParams().height = expandedHeight;
        upsellButton.setTranslationY(0F);
        upsellText.setTranslationY(0F);
        requestLayout();
    }

}
