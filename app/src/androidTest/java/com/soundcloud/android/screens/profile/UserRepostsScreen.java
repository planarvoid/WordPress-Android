package com.soundcloud.android.screens.profile;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.profile.UserRepostsActivity;

public class UserRepostsScreen extends BaseUserTracksScreen {

    public UserRepostsScreen(Han testDriver) {
        super(testDriver);
    }

    @Override
    protected Class getActivity() {
        return UserRepostsActivity.class;
    }
}