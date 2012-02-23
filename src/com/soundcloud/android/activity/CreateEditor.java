package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.record.RawAudioPlayer;
import com.soundcloud.android.view.create.EditWaveformLayout;
import com.soundcloud.android.view.create.EditWaveformView;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class CreateEditor extends ScActivity{

    EditWaveformView mWaveformView;
    EditWaveformLayout mWaveformLayout;

    RawAudioPlayer mRawAudioPlayer;

    private boolean mShowingSmoothProgress;
    private long mProgressPeriod = 1000 / 60; // aim for 60 fps.

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.sc_create_edit);

        // setup views
        findViewById((R.id.btn_play)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRawAudioPlayer.togglePlayback();
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

        mRawAudioPlayer = new RawAudioPlayer();
        mWaveformLayout = (EditWaveformLayout) findViewById(R.id.waveform_layout);
        mWaveformLayout.setPlayer(mRawAudioPlayer);

        // this will come from an intent, but for now just going to a play file on my sd card
        setFile(new File(Environment.getExternalStorageDirectory(),"med_test.wav"));
    }

    private void setFile(File f){
        mWaveformView = mWaveformLayout.setWaveformFromFile(f);
        mRawAudioPlayer.setFile(f);
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
            mWaveformView.setCurrentProgress(mRawAudioPlayer.getCurrentProgressPercent());
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
