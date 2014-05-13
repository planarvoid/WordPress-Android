package com.soundcloud.android.playback.ui;

import com.soundcloud.android.actionbar.ActionBarController;

import android.app.Activity;
import android.os.Bundle;

public class LegacyPlayerController implements PlayerController {

    @Override
    public void attach(Activity activity, ActionBarController actionBarController) {}

    @Override
    public boolean isExpanded() {
        return false;
    }

    @Override
    public void collapse() {}

    @Override
    public void storeState(Bundle bundle) {}

    @Override
    public void restoreState(Bundle bundle) {}

}
