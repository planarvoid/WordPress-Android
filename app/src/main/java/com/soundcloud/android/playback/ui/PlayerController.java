package com.soundcloud.android.playback.ui;

import com.soundcloud.android.actionbar.ActionBarController;

import android.app.Activity;
import android.os.Bundle;

public interface PlayerController {

    void attach(Activity activity, ActionBarController actionBarController);


    boolean isExpanded();

    void collapse();

    void onResume();

    boolean handleBackPressed();

    void onPause();

    void storeState(Bundle bundle);

    void restoreState(Bundle bundle);

    void expand();
}
