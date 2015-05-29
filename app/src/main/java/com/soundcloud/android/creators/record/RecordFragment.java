package com.soundcloud.android.creators.record;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class RecordFragment extends LightCycleSupportFragment {
    public static Fragment create() {
        return new RecordFragment();
    }

    @Inject @LightCycle RecordPresenter recordPresenter;

    public RecordFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sc_create, container, false);
    }

    public enum CreateState {
        IDLE_RECORD(R.string.rec_title_idle_rec),
        RECORD(R.string.rec_title_recording),
        IDLE_PLAYBACK(R.string.rec_title_idle_play),
        PLAYBACK(R.string.rec_title_playing),
        EDIT(R.string.rec_title_editing),
        EDIT_PLAYBACK(R.string.rec_title_editing);

        private final int titleId;

        CreateState(int titleId) {
            this.titleId = titleId;
        }

        public boolean isEdit() {
            return this == EDIT || this == EDIT_PLAYBACK;
        }

        public boolean isPlayState() {
            return this == PLAYBACK || this == EDIT_PLAYBACK;
        }

        public int getTitleId() {
            return titleId;
        }
    }

}
