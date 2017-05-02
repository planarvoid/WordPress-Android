package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import android.view.View;

import javax.inject.Inject;

public class VisualPrestitialActivity extends LoggedInActivity {

    public static final String EXTRA_AD = "EXTRA_AD";

    @Inject @LightCycle VisualPrestitialPresenter presenter;

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
