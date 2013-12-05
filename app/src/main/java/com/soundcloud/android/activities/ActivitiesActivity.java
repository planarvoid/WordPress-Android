package com.soundcloud.android.activities;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.rx.Event;
import com.soundcloud.android.storage.provider.Content;

import android.os.Bundle;

public class ActivitiesActivity extends ScActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        if (state == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(getContentHolderViewId(), ScListFragment.newInstance(Content.ME_ACTIVITIES)).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isConfigurationChange() || isReallyResuming()) {
            Event.SCREEN_ENTERED.publish(Screen.ACTIVITIES.get());
        }
    }

}
