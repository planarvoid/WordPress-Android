package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.provider.Content;

import android.os.Bundle;

public class News extends ScActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                    //.add(mRootView.getContentHolderId(), ActivitiesFragment.create(Content.ME_ACTIVITIES)).commit();
                    .add(R.id.content_frame, ScListFragment.newInstance(Content.ME_ACTIVITIES)).commit();
        }
    }
}
