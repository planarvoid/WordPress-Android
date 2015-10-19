package com.soundcloud.android.screens;

import static com.soundcloud.android.framework.with.With.text;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class ExpandedProfileImageScreen extends Screen {

    public ExpandedProfileImageScreen(Han solo) {
        super(solo);
    }

    @Override
    public boolean isVisible() {
        return testDriver
                .findElement(With.id(R.id.profile_expanded_image))
                .findElement(With.id(R.id.image))
                .isVisible();
    }

    @Override
    protected Class getActivity() {
        return ProfileActivity.class;
    }

}
