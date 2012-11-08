package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.provider.Content;
import net.hockeyapp.android.UpdateManager;

import android.os.Bundle;

public class Stream extends ScActivity implements ScLandingPage {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getSupportActionBar().setTitle(getString(R.string.tab_stream));

        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(mRootView.getContentHolderId(), ScListFragment.newInstance(Content.ME_SOUND_STREAM)).commit();
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_stream;
    }

    @Override
    public LandingPage getPageValue() {
        return LandingPage.Stream;
    }
}
