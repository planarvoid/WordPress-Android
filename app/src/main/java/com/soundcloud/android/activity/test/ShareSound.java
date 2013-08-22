package com.soundcloud.android.activity.test;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;

public class ShareSound extends Activity {

    public static final String TAG = ShareSound.class.getSimpleName();
    private Intent mIntent;
    private Result result;

    public static final String SHARE = "Share";
    public static final String SHARE_IMPLICIT = "Share implicit";
    public static final String SHARE_TO = "Share to";


    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.HORIZONTAL);

        Button share = new Button(this);
        Button shareImplicit = new Button(this);

        share.setText(SHARE);
        shareImplicit.setText(SHARE_IMPLICIT);

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIntent != null) {
                    startActivityForResult(mIntent, 0);
                }
            }
        });

        shareImplicit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File audio = new File("/path/to/audio.mp3");
                Intent intent = new Intent(Intent.ACTION_SEND).setType("audio/*");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(audio));
                startActivityForResult(Intent.createChooser(intent, SHARE_TO), 0);
            }
        });

        view.addView(share);
        view.addView(shareImplicit);
        setContentView(view);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult("+requestCode+", "+resultCode+", "+data);
        result = new Result(requestCode, resultCode, data);
    }

    public void setIntent(Intent intent) {
        mIntent = intent;
    }

    public Result getResult() { return result; }

    public static class Result {
        public Result(int requestCode, int resultCode, Intent intent) {
            this.resultCode = resultCode;
            this.requestCode = requestCode;
            this.intent = intent;
        }

        public int resultCode, requestCode;
        public Intent intent;
    }
}