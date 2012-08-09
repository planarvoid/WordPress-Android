package com.soundcloud.android.activity.test;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

public class ShareSound extends Activity {

    public static final String TAG = ShareSound.class.getSimpleName();
    private Intent mIntent;
    private Result result;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        ViewGroup view = new FrameLayout(this);

        Button share = new Button(this);
        share.setText("Share");
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIntent != null) {
                    startActivityForResult(mIntent, 0);
                }
            }
        });
        view.addView(share);
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