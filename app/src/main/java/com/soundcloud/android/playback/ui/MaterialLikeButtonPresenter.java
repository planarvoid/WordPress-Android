package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.CenteredImageSpan;

import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.widget.ToggleButton;

import javax.inject.Inject;

public class MaterialLikeButtonPresenter implements LikeButtonPresenter {

    private final CondensedNumberFormatter numberFormatter;

    @Inject
    public MaterialLikeButtonPresenter(CondensedNumberFormatter numberFormatter) {
        this.numberFormatter = numberFormatter;
    }

    @Override
    public void setLikeCount(ToggleButton likeButton, int count) {
        likeButton.setTextOn(createLikeSpan(likeButton, count, R.drawable.player_like_active));
        likeButton.setTextOff(createLikeSpan(likeButton, count, R.drawable.player_like));
        // hax : setTextOn/Off does not sync text state. Call this to refresh the text (count)
        likeButton.setChecked(likeButton.isChecked());
    }

    @NonNull
    private SpannableString createLikeSpan(ToggleButton likeButton, int count, int player_like_active) {
        ImageSpan imageSpanOn = new CenteredImageSpan(likeButton.getContext(), player_like_active);
        SpannableString contentOn = new SpannableString(count > 0 ? "X " + numberFormatter.format(count) : "X");
        contentOn.setSpan(imageSpanOn, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return contentOn;
    }
}
