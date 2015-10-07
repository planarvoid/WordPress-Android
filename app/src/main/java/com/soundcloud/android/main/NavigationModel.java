package com.soundcloud.android.main;

import com.soundcloud.android.analytics.Screen;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import java.util.Arrays;
import java.util.List;

public class NavigationModel {

    private final List<Target> listItems;

    public NavigationModel(Target... listItems) {
        this.listItems = Arrays.asList(listItems);
    }

    public Target getItem(int position) {
        return listItems.get(position);
    }

    public int getItemCount() {
        return listItems.size();
    }

    public interface Target {

        @StringRes
        int getName();

        @DrawableRes
        int getIcon();

        Fragment createFragment();

        Screen getScreen();
    }

}