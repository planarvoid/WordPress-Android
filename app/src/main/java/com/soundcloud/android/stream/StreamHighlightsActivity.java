package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class StreamHighlightsActivity extends PlayerActivity {

    public static final String URN_ARGS = "Urns";

    @Inject BaseLayoutHelper baseLayoutHelper;

    public StreamHighlightsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            attachFragment();
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.STREAM_HIGHLIGHTS;
    }

    private void attachFragment() {
        StreamHighlightsFragment fragment = new StreamHighlightsFragment();
        fragment.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

}
