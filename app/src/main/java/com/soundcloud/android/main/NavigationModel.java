package com.soundcloud.android.main;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import java.util.Arrays;
import java.util.List;

public class NavigationModel {

    public static final int NOT_FOUND = -1;

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

    public int getPosition(Screen screen) {
        for (int i = 0; i < listItems.size(); i++) {
            if (listItems.get(i).getScreen().equals(screen)) {
                return i;
            }
        }
        return NOT_FOUND;
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
