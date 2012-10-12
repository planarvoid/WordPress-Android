package com.soundcloud.android.activity;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.ScListView;
import com.viewpagerindicator.TabPageIndicator;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class News extends ScListActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(mRootView.getContentHolderId(), ScListFragment.newInstance(Content.ME_ACTIVITIES)).commit();
        }
    }
}
