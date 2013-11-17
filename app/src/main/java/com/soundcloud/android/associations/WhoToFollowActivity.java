package com.soundcloud.android.associations;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.storage.provider.Content;

import android.os.Bundle;

public class WhoToFollowActivity extends ScActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(getString(R.string.side_menu_who_to_follow));
        if (state == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, ScListFragment.newInstance(Content.SUGGESTED_USERS))
                    .commit();
        }
    }
}