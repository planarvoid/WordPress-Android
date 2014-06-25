package com.soundcloud.android.playback.ui;

import com.soundcloud.android.actionbar.ActionBarController;

import android.app.Activity;
import android.os.Bundle;

public class LegacyPlayerController implements PlayerController {

    @Override
    public void attach(Activity activity, ActionBarController actionBarController) {
        // Do nothing
    }

    @Override
    public void onResume() {
        // Do nothing
    }

    @Override
    public void onPause() {
        // Do nothing
    }

    @Override
    public boolean isExpanded() {
        return false;
    }

    @Override
    public void expand() {
        // Do nothing
    }

    @Override
    public void collapse() {
        // Do nothing
    }

    @Override
    public void storeState(Bundle bundle) {
        // Do nothing
    }

    @Override
    public void restoreState(Bundle bundle) {
        // Do nothing
    }

}
