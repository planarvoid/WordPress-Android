package com.soundcloud.android.main;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

public abstract class BaseNavigationTarget implements NavigationModel.Target {

    private final int name;
    private final int icon;

    public BaseNavigationTarget(@StringRes int name, @DrawableRes int icon) {
        this.name = name;
        this.icon = icon;
    }

    @Override
    public int getName() {
        return name;
    }

    @Override
    public int getIcon() {
        return icon;
    }

}
