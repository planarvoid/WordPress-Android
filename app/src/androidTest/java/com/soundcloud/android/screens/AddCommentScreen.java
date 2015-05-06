package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.with.With;

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

    public String getTitle() {
        return new TextElement(testDriver.findElement(With.id(android.R.id.title))).getText();
    }

}
