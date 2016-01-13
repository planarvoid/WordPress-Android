package com.soundcloud.android.playback.ui;

import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.strings.Strings;

import android.widget.ToggleButton;

import javax.inject.Inject;

public class CompatLikeButtonPresenter implements LikeButtonPresenter {

    private final CondensedNumberFormatter numberFormatter;

    @Inject
    public CompatLikeButtonPresenter(CondensedNumberFormatter numberFormatter) {
        this.numberFormatter = numberFormatter;
    }

    @Override
    public void setLikeCount(ToggleButton likeButton, int count) {
        likeButton.setText(count > 0 ? numberFormatter.format(count) : Strings.EMPTY);
    }
}
