package com.soundcloud.android.playback.ui;

import com.soundcloud.android.actionbar.ActionBarController;

import android.app.Activity;

public interface PlayerController {

    void attach(Activity activity, ActionBarController actionBarController);

    boolean isExpanded();

    void collapse();

}
