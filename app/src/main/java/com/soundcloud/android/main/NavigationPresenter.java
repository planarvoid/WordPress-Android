package com.soundcloud.android.main;

import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.support.v7.app.AppCompatActivity;

public abstract class NavigationPresenter extends DefaultActivityLightCycle<AppCompatActivity>
        implements LegacyNavigationFragment.NavigationCallbacks {

    @Override
    public void onSmoothSelectItem(LegacyNavigationFragment.NavItem item) {}

    @Override
    public void onSelectItem(LegacyNavigationFragment.NavItem item) {}

    public boolean handleBackPressed() {
        return false;
    }

    public void onInvalidateOptionsMenu() {}

    public abstract void setBaseLayout(AppCompatActivity activity);

    public abstract void trackScreen();

}
