package com.soundcloud.android.activity;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.Uri;
import android.util.Log;
import com.soundcloud.android.R;
import com.soundcloud.android.model.Connection;
import com.soundcloud.android.model.User;
import com.soundcloud.android.task.fetch.FetchUserTask;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.record.RawAudioPlayer;
import com.soundcloud.android.view.create.WaveformView;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import java.io.File;
import java.util.List;

public class CreateEditor extends ScActivity{

    WaveformView mWaveformView;
    RawAudioPlayer mRawAudioPlayer;

    private boolean mShowingSmoothProgress;
    private long mProgressPeriod = 30;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.sc_create_edit);

        mWaveformView = (WaveformView) findViewById(R.id.waveform_view);

        Object state = getLastNonConfigurationInstance();

        final File f = new File(Environment.getExternalStorageDirectory(),"med_test.wav");

        if (state != null){
            mWaveformView.restoreConfigurationInstance(state);
        } else {
           mWaveformView.setFromFile(f);
        }

        mRawAudioPlayer = new RawAudioPlayer();
        findViewById((R.id.btn_play)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRawAudioPlayer.play(f);
                startSmoothProgress();
            }
        });

        findViewById((R.id.btn_stop)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRawAudioPlayer.stop();
                stopSmoothProgress();
            }
        });


    }

    private Runnable mSmoothProgress = new Runnable() {
        public void run() {
            mWaveformView.setCurrentProgress(mRawAudioPlayer.getCurrentProgress());
            mHandler.postDelayed(this, mProgressPeriod);
        }
    };

    public void startSmoothProgress(){
        mShowingSmoothProgress = true;
        mHandler.postDelayed(mSmoothProgress, 0);
    }

    public void stopSmoothProgress(){
        mShowingSmoothProgress = false;
        mHandler.removeCallbacks(mSmoothProgress);
    }


    @Override
    public Object onRetainNonConfigurationInstance() {
        return mWaveformView.saveConfigurationInstance();
    }


}
