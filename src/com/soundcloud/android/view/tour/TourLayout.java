package com.soundcloud.android.view.tour;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

public class TourLayout extends FrameLayout {

    private SoundCloudApplication mApp;
    private String mTitle;


    public TourLayout(Context context) {
        super(context);
    }

    protected void init(String title) {
        mTitle = title;
        //((TextView) findViewById(R.id.txt_title)).setText(title);
        //((RadioButton) ((RadioGroup) findViewById(R.id.rdo_tour_step)).getChildAt(tourIndex)).setChecked(true);
    }

    public String getTitle(){
        return mTitle;
    }
}
