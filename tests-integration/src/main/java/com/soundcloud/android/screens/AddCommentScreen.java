package com.soundcloud.android.screens;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.viewelements.TextElement;
import com.soundcloud.android.tests.with.With;

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
        return new TextElement(testDriver.findElement(With.id(eu.inmite.android.lib.dialogs.R.id.sdl__title)))
                .getText();
    }

}
