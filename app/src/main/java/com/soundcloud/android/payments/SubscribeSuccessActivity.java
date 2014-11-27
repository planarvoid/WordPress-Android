package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;

public class SubscribeSuccessActivity extends ScActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.subscribe_success_activity);
    }

    @Override
    protected ActionBarController createActionBarController() {
        // No overflow or search
        return null;
    }

}
