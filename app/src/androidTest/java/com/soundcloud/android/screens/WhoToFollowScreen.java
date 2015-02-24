package com.soundcloud.android.screens;

import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.framework.Han;

public class WhoToFollowScreen extends Screen {

    public WhoToFollowScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return WhoToFollowActivity.class;
    }
}
