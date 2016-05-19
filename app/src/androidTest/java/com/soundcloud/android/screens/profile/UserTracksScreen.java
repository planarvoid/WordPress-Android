package com.soundcloud.android.screens.profile;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.profile.UserTracksActivity;

public class UserTracksScreen extends BaseUserTracksScreen {

    public UserTracksScreen(Han testDriver) {
        super(testDriver);
    }

    @Override
    protected Class getActivity() {
        return UserTracksActivity.class;
    }
}
