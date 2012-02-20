package com.soundcloud.android.activity;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.R;
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

        setContentView(R.layout.sc_create_edit);

        mWaveformView = (WaveformView) findViewById(R.id.waveform_view);

        Object state = getLastNonConfigurationInstance();

        if (state != null){
            mWaveformView.restoreConfigurationInstance(state);
        } else {
           mWaveformView.setFromFile(new File(Environment.getExternalStorageDirectory(),"fredp.wav"));
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mWaveformView.saveConfigurationInstance();
    }


}
