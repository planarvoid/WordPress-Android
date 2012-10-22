package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.provider.Content;

import android.os.Bundle;

public class Stream extends ScListActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(mRootView.getContentHolderId(), ScListFragment.newInstance(Content.ME_SOUND_STREAM)).commit();
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_stream;
    }
}
