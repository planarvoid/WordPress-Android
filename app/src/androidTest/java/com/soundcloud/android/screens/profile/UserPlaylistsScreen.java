package com.soundcloud.android.screens.profile;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.profile.UserPlaylistsActivity;

public class UserPlaylistsScreen extends BaseUserPlaylistsScreen {

    public UserPlaylistsScreen(Han testDriver) {
        super(testDriver);
    }

    @Override
    protected Class getActivity() {
        return UserPlaylistsActivity.class;
    }
}
