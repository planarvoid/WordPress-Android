package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.dialog.OnboardSuggestedUsersSyncFragment;

import android.os.Bundle;

public class SuggestedUsersSyncActivity extends ScActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.frame_layout_holder);

        if (state == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.holder, new OnboardSuggestedUsersSyncFragment())
                    .commit();
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return R.menu.onboard;
    }
}
