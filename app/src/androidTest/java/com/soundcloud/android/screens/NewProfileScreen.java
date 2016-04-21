package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.elements.Tabs;

public class NewProfileScreen extends ProfileScreen {
    public NewProfileScreen(Han solo) {
        super(solo);
    }

    @Override
    public ProfileScreen touchLegacyFollowingsTab() {
        final Tabs tabs = tabs();
        tabs.getTabWith(text(testDriver.getString(R.string.tab_title_user_followings))).click();
        waiter.waitForContentAndRetryIfLoadingFailed();
        return this;
    }
}
