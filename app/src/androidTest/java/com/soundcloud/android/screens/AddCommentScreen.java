package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.main.MainActivity;

public class AddCommentScreen extends Screen {

    public AddCommentScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }

    public boolean waitForDialog() {
        return waiter.waitForFragmentByTag("add_comment_dialog");
    }

}
