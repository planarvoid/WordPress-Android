package com.soundcloud.android.activity.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ActionBarController;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuInflater;

public class Legal extends PreferenceActivity implements ActionBarController.ActionBarOwner {
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
    public ActionBar getSupportActionBar() {
        return null;
    }

    @Override
    public MenuInflater getSupportMenuInflater() {
        return null;
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.main;
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}