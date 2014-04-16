package com.soundcloud.android.accounts;

import com.soundcloud.android.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class LogoutActivity extends FragmentActivity {

    @TargetApi(11)
    public static void start(Activity activity) {
        Intent intent = new Intent(activity, LogoutActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // so that at least on newer devices, backing out of this actually exits the app
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logout_activity);
    }
}
