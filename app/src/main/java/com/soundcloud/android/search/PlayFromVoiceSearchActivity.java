package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.main.RootActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

@SuppressWarnings({"PMD.AccessorClassGeneration"})
public class PlayFromVoiceSearchActivity extends RootActivity {
    @Inject @LightCycle PlayFromVoiceSearchPresenter presenter;

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.resolve);
    }

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }

}


