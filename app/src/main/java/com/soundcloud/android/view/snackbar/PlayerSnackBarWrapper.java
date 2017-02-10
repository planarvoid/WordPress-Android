package com.soundcloud.android.view.snackbar;

import com.soundcloud.android.R;

import android.content.res.Resources;
import android.graphics.Color;

import javax.inject.Inject;

class PlayerSnackBarWrapper extends SnackBarWrapper {
    @Inject
    public PlayerSnackBarWrapper(Resources resources) {
        super(resources.getColor(R.color.snack_bar_bg_dark), Color.WHITE);
    }
}
