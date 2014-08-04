package com.soundcloud.android.main;

import com.soundcloud.android.actionbar.ActionBarController;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public interface LifeCycleComponent {
    void attach(Activity activity, ActionBarController actionBarController);
    void onCreate(Bundle bundle);
    void onNewIntent(Intent intent);
    void onStart();
    void onResume();
    void onPause();
    void onStop();
    void onSaveInstanceState(Bundle bundle);
    void onRestoreInstanceState(Bundle bundle);
    void onDestroy();
}
