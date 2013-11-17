package com.soundcloud.android.activities;

import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.storage.provider.Content;

import android.os.Bundle;

public class ActivitiesActivity extends ScActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                    //.add(mRootView.getContentHolderId(), ActivitiesFragment.create(Content.ME_ACTIVITIES)).commit();
                    .add(android.R.id.content, ScListFragment.newInstance(Content.ME_ACTIVITIES)).commit();
        }
    }
}
