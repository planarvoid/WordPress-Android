package com.soundcloud.android.screens;


import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.profile.FollowingsActivity;

public class FollowingsScreen extends Screen {

    FollowingsScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return FollowingsActivity.class;
    }

    public ProfileScreen goBackToProfile() {
        testDriver.goBack();
        return new ProfileScreen(testDriver);
    }

    public boolean showsEmptyMessage() {
        return testDriver.findOnScreenElement(With.text(R.string.new_empty_user_followings_text)).hasVisibility();
    }
}
