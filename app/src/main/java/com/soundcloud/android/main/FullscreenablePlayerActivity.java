package com.soundcloud.android.main;

import static com.soundcloud.android.view.status.StatusBarUtils.getStatusBarHeight;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.soundcloud.android.R;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

public abstract class FullscreenablePlayerActivity extends PlayerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (shouldBeFullscreen()) {
            View panel = findViewById(R.id.player_root);
            ((SlidingUpPanelLayout.LayoutParams) panel
                    .getLayoutParams()).setMargins(0, getStatusBarHeight(this), 0, 0);
        }
    }

    @Override
    protected void setActivityContentView() {
        super.setActivityContentView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && shouldBeFullscreen()) {
            setTheme(R.style.Theme_SoundCloud_TransparentStatus);
            setStableFullscreen();
        } else {
            setTheme(R.style.Theme_SoundCloud);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setStableFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    protected abstract boolean shouldBeFullscreen();
}
