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

    public TrackLikesScreen clickKeepLikesSynced() {
        testDriver.findElement(text(testDriver.getString(R.string.make_offline_available))).click();
        return new TrackLikesScreen(testDriver);
    }

    public TrackLikesScreen clickKeepLikesSyncedAndWaitToFinish() {
        testDriver.findElement(text(testDriver.getString(R.string.make_offline_available))).click();
        TrackLikesScreen trackLikesScreen = new TrackLikesScreen(testDriver);
        trackLikesScreen.waitForLikesDownloadToFinish();
        return trackLikesScreen;
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }
}
