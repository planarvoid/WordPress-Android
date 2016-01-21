package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;

public class SyncYourLikesScreen extends Screen {

    public SyncYourLikesScreen(Han solo) {
        super(solo);
        waiter.waitForElementToBeVisible(text(testDriver.getString(R.string.offline_likes_dialog_title)));
    }

    @Override
    public boolean isVisible() {
        return testDriver.findOnScreenElement(text(testDriver.getString(R.string.offline_likes_dialog_title))).isVisible();
    }

    public TrackLikesScreen clickKeepLikesSynced() {
        makeAvailableOfflineButton().click();
        return new TrackLikesScreen(testDriver);
    }

    public TrackLikesScreen clickKeepLikesSyncedAndWaitToFinish() {
        testDriver.findOnScreenElement(text(testDriver.getString(R.string.make_offline_available))).click();
        TrackLikesScreen trackLikesScreen = new TrackLikesScreen(testDriver);
        trackLikesScreen.waitForLikesDownloadToFinish();
        return trackLikesScreen;
    }

    private ViewElement content() {
        int titleId = testDriver.getResources().getIdentifier( "contentPanel", "id", "android" );
        return testDriver.findOnScreenElement(With.id(titleId));
    }

    private ViewElement makeAvailableOfflineButton() {
        return testDriver.findOnScreenElement(text(testDriver.getString(R.string.make_offline_available)));
    }

    @Override
    protected Class getActivity() {
        return MainActivity.class;
    }
}
