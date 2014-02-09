package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.Han;

public class LikesScreen extends Screen {
    protected Class ACTIVITY = MainActivity.class;

    public LikesScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
