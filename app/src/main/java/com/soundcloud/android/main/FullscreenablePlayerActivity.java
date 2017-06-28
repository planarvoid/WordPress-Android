package com.soundcloud.android.main;

import com.soundcloud.android.R;

import android.view.View;

public abstract class FullscreenablePlayerActivity extends PlayerActivity {

    @Override
    protected void setActivityContentView() {
        super.setActivityContentView();

        if (shouldBeFullscreen()) {
            setTheme(R.style.Theme_SoundCloud_TransparentStatus);
            setStableFullscreen();
        } else {
            setTheme(R.style.Theme_SoundCloud);
        }
    }

    private void setStableFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    protected abstract boolean shouldBeFullscreen();
}
