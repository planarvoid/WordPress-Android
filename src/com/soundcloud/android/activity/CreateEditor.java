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
import android.widget.Button;

import java.io.File;
import java.util.List;

public class CreateEditor extends ScActivity{

    WaveformView mWaveformView;
    RawAudioPlayer mRawAudioPlayer;

    private boolean mShowingSmoothProgress;
    private long mProgressPeriod = 1000 / 60; // aim for 60 fps.

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.sc_create_edit);

        // this will come from an intent, but for now just going to a wave file on my sd card
        final File f = new File(Environment.getExternalStorageDirectory(),"med_test.wav");

        // setup wave viewer
        mWaveformView = (WaveformView) findViewById(R.id.waveform_view);
        mWaveformView.setFromFile(f);

        // setup wave player
        mRawAudioPlayer = new RawAudioPlayer();
        mRawAudioPlayer.setFile(f);

        // setup buttons
        findViewById((R.id.btn_play)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRawAudioPlayer.togglePlayback(mWaveformView.getCurrentProgress());
                setCurrentPlayState();
            }
        });
        findViewById((R.id.btn_stop)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRawAudioPlayer.stop();
                setCurrentPlayState();
                mWaveformView.setCurrentProgress(0);
            }
        });

        setCurrentPlayState();
    }

    private void setCurrentPlayState(){
        if (mRawAudioPlayer.isPlaying()){
            ((Button) findViewById((R.id.btn_play))).setText("pause");
            if (!mShowingSmoothProgress) startSmoothProgress();
        } else {
            ((Button) findViewById((R.id.btn_play))).setText("play");
            if (mShowingSmoothProgress) stopSmoothProgress();
        }
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

}
