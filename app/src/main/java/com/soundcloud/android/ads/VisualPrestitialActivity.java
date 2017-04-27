package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;

import android.view.View;

public class VisualPrestitialActivity extends LoggedInActivity {

    public static final String EXTRA_AD = "EXTRA_AD";

    public VisualPrestitialActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    private void hideStatusBar() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStatusBar();
    }

    @Override
    public Screen getScreen() {
        return Screen.VISUAL_PRESTITIAL;
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.visual_prestitial);
    }
}
