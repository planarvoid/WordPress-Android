package com.soundcloud.android.view.snackbar;

import com.soundcloud.android.R;

import android.content.res.Resources;
import android.graphics.Color;

import javax.inject.Inject;

class DefaultSnackBarWrapper extends SnackBarWrapper {
    @Inject
    public DefaultSnackBarWrapper(Resources resources) {
        super(resources.getColor(R.color.snack_bar_bg_light), Color.DKGRAY);
    }
}
