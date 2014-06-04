package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.Han;

import android.widget.ListView;

public class LikesScreen extends Screen {
    protected static final Class ACTIVITY = MainActivity.class;

    public LikesScreen(Han solo) {
        super(solo);
    }

    public void clickItem(int index) {
        ListView listView = solo.getCurrentListView();
        solo.clickOnView(listView.getChildAt(index));
    }
    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
