package com.soundcloud.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.view.create.WaveformView;

import android.os.Bundle;
import android.os.Environment;

import java.io.File;
import java.util.List;

public class CreateEditor extends ScActivity{

    WaveformView mWaveformView;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mWaveformView = new WaveformView(this);
        setContentView(mWaveformView);

        Object state = getLastNonConfigurationInstance();

        if (state != null){
            mWaveformView.restoreConfigurationInstance(state);
        } else if (getIntent().hasExtra(Intent.EXTRA_STREAM)){
            Uri stream = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            File file = IOUtils.getFromMediaUri(getContentResolver(), stream);
            if (file != null){
                mWaveformView.setFromFile(file);
            }

        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mWaveformView.saveConfigurationInstance();
    }


}
