package com.soundcloud.android.creators.upload;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UploadMonitorFragment extends LightCycleSupportFragment<UploadMonitorFragment> {

    @Inject LeakCanaryWrapper leakCanaryWrapper;
    @Inject @LightCycle UploadMonitorPresenter uploadMonitorPresenter;

    public static Fragment create(Recording recording) {
        final UploadMonitorFragment uploadMonitorFragment = new UploadMonitorFragment();
        Bundle args = new Bundle();
        args.putParcelable(UploadMonitorPresenter.RECORDING_KEY, recording);
        uploadMonitorFragment.setArguments(args);
        return uploadMonitorFragment;
    }

    public UploadMonitorFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.upload_monitor, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        leakCanaryWrapper.watch(this);
    }
}
