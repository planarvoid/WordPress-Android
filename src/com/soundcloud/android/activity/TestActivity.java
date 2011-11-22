package com.soundcloud.android.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.soundcloud.android.R;

public class TestActivity extends FragmentActivity {

     @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.user_favorites_test);
     }

}
