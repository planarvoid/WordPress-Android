package com.soundcloud.android.framework.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.screens.explore.ExploreScreen;
import com.soundcloud.android.framework.Han;

public class MainScreen extends Screen {
    private static final Class ACTIVITY = MainActivity.class;

    private MenuScreen menuScreen;

    public MainScreen(Han solo) {
        super(solo);
        menuScreen = new MenuScreen(solo);
    }

    public ExploreScreen openExploreFromMenu() {
        return menuScreen.open()
                .clickExplore();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
