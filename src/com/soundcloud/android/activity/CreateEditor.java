package com.soundcloud.android.activity;

import com.soundcloud.android.view.create.WaveformView;

import android.os.Bundle;
import android.os.Environment;

import java.io.File;

public class CreateEditor extends ScActivity{
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        WaveformView wv = new WaveformView(this);
        setContentView(wv);

        wv.setFromFile(new File(Environment.getExternalStorageDirectory(),"fredp.wav"));
    }
}
