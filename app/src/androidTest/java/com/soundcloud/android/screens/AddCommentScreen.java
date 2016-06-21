package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.main.MainActivity;

public class AddCommentScreen extends Screen {

    public static final String FRAGMENT_TAG = "add_comment_dialog";

    public AddCommentScreen(Han solo) {
        super(solo);
        waiter.assertForFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    public boolean waitForDialog() {
        return waiter.waitForFragmentByTag(FRAGMENT_TAG);
    }

}
