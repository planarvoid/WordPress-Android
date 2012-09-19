package com.soundcloud.android.view.tour;

import com.soundcloud.android.R;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public abstract class TourLayout extends FrameLayout {
    public TourLayout(Context context, int resId) {
        super(context);
        View.inflate(context, resId, this);
    }

    public CharSequence getMessage() {
        TextView text = (TextView) findViewById(R.id.txt_message);
        return text.getText();
    }
}
