package com.soundcloud.android.activity.settings;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.soundcloud.android.R;
import com.soundcloud.android.activity.ActionBarController;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.os.Bundle;

public class Legal extends SherlockPreferenceActivity implements ActionBarController.ActionBarOwner {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.legal);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @NotNull
    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.main;
    }

    @Override
    public void onHomePressed() {
    }

    @Override
    public void block() {
    }

    @Override
    public void unblock(boolean instantly) {
    }
}