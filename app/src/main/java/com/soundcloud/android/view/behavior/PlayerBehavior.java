package com.soundcloud.android.view.behavior;

import com.google.auto.factory.AutoFactory;

import android.support.design.widget.BottomSheetBehavior;
import android.view.View;

/**
 * Wrapper class for {@link android.support.design.widget.BottomSheetBehavior}'s properties that cannot be mocked.
 */
@AutoFactory(allowSubclasses = true)
public class PlayerBehavior {

    private BottomSheetBehavior<View> bottomSheetBehavior;

    PlayerBehavior(BottomSheetBehavior<View> bottomSheetBehavior) {
        this.bottomSheetBehavior = bottomSheetBehavior;
    }

    int getPeekHeight() {
        return bottomSheetBehavior.getPeekHeight();
    }

    int getState() {
        return bottomSheetBehavior.getState();
    }
}
