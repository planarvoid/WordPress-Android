package com.soundcloud.android.screens;

import com.soundcloud.android.tests.Han;

public class HomeScreen {
    private Han solo;

    public HomeScreen(Han solo) {
        this.solo = solo;
    }

    public boolean hasItemByUsername(String username){
        return solo.searchText(username, true);
    }
}
