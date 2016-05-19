package com.soundcloud.android.screens.profile;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.profile.UserAlbumsActivity;

public class UserAlbumsScreen extends BaseUserPlaylistsScreen {

    public UserAlbumsScreen(Han testDriver) {
        super(testDriver);
    }

    @Override
    protected Class getActivity() {
        return UserAlbumsActivity.class;
    }
}
