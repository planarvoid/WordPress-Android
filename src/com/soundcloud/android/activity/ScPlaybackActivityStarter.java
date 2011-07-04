package com.soundcloud.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ScPlaybackActivityStarter extends Activity
{
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        Intent i = new Intent(getIntent());
        i.setClass(this, ScPlayer.class);
        i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
        finish();
    }
}

