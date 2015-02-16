package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.main.MainActivity;

public class SyncYourLikesScreen extends Screen {

    public SyncYourLikesScreen(Han solo) {
        super(solo);
    }

    @Override
    public boolean isVisible() {
        return testDriver.findElement(text(testDriver.getString(R.string.offline_likes_dialog_title))).isVisible();
    }

    public void clickKeepLikesSynced() {
        testDriver.findElement(text(testDriver.getString(R.string.offline_likes_dialog_accept))).click();
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }
}
