package com.soundcloud.android.creators.upload;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class MetadataFragment extends LightCycleSupportFragment<MetadataFragment> {

    @Inject @LightCycle MetadataPresenter metadataPresenter;
    @Inject LeakCanaryWrapper leakCanaryWrapper;

    public static Fragment create(Recording recording) {
        final MetadataFragment metadataFragment = new MetadataFragment();
        Bundle args = new Bundle();
        args.putParcelable(MetadataPresenter.RECORDING_KEY, recording);
        metadataFragment.setArguments(args);
        return metadataFragment;
    }

    public static Fragment create() {
        return new MetadataFragment();
    }

    public MetadataFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sc_upload, container, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        metadataPresenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        metadataPresenter.onRequestPermissionsResult(requestCode, grantResults);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        leakCanaryWrapper.watch(this);
    }
}
