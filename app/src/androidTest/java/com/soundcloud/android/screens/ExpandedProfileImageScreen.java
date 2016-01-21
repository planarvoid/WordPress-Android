package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.profile.ProfileActivity;

public class ExpandedProfileImageScreen extends Screen {

    public ExpandedProfileImageScreen(Han solo) {
        super(solo);
    }

    @Override
    public boolean isVisible() {
        return testDriver
                .findOnScreenElement(With.id(R.id.full_image))
                .findOnScreenElement(With.id(R.id.image))
                .isVisible();
    }

    @Override
    protected Class getActivity() {
        return ProfileActivity.class;
    }

}
