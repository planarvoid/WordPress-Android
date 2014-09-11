package com.soundcloud.android.main;

import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public interface ActivityLifeCycle<ActivityT extends Activity> {
    void onBind(ActivityT owner);
    void onCreate(@Nullable Bundle bundle);
    void onNewIntent(Intent intent);
    void onStart();
    void onResume();
    void onPause();
    void onStop();
    void onSaveInstanceState(Bundle bundle);
    void onRestoreInstanceState(Bundle bundle);
    void onDestroy();
}
