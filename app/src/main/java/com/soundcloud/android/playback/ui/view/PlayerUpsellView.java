package com.soundcloud.android.playback.ui.view;

import com.soundcloud.android.R;
import com.soundcloud.android.cast.RedrawLayoutListener;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

public class PlayerUpsellView extends RelativeLayout {

    private final TextView upsellText;
    private final Button upsellButton;

    private final int expandedHeight;
    private final int collapsedHeight;
    private final int animTranslationY;

    public PlayerUpsellView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.player_upsell, this, true);

        upsellText = (TextView) findViewById(R.id.upsell_text);
        upsellButton = (Button) findViewById(R.id.upsell_button);
        expandedHeight = getResources().getDimensionPixelSize(R.dimen.player_upsell_height);
        animTranslationY = getResources().getDimensionPixelOffset(R.dimen.player_upsell_anim_translation_y);
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

    public List<ValueAnimator> getCollapseAnimators() {
        final ObjectAnimator buttonAnimator = ObjectAnimator.ofFloat(upsellButton, View.TRANSLATION_Y, 0f, animTranslationY);
        final ObjectAnimator textViewAnimator = ObjectAnimator.ofFloat(upsellText, View.TRANSLATION_Y, 0f, animTranslationY);
        return Arrays.asList(buttonAnimator, textViewAnimator, createRedrawnAnimator(expandedHeight, 0));
    }

    public List<ValueAnimator> getExpandAnimators() {
        final ObjectAnimator buttonAnimator = ObjectAnimator.ofFloat(upsellButton, View.TRANSLATION_Y, animTranslationY, 0f);
        final ObjectAnimator textViewAnimator = ObjectAnimator.ofFloat(upsellText, View.TRANSLATION_Y, animTranslationY, 0f);
        return Arrays.asList(buttonAnimator, textViewAnimator, createRedrawnAnimator(0, expandedHeight));
    }

    private ValueAnimator createRedrawnAnimator(int from, int to) {
        final ValueAnimator upsellExpand = ObjectAnimator.ofInt(from, to);
        upsellExpand.addUpdateListener(new RedrawLayoutListener(this));
        return upsellExpand;
    }
}
