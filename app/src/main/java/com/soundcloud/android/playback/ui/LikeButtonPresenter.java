package com.soundcloud.android.playback.ui;

import android.support.annotation.DrawableRes;
import android.widget.ToggleButton;

public interface LikeButtonPresenter {
    void setLikeCount(ToggleButton likeButton, int count,
                      @DrawableRes int drawableLiked, @DrawableRes int drawableUnliked);
}
